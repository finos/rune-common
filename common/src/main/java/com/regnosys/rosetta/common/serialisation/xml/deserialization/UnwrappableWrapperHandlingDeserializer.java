package com.regnosys.rosetta.common.serialisation.xml.deserialization;

/*-
 * ==============
 * Rune Common
 * ==============
 * Copyright (C) 2018 - 2025 REGnosys
 * ==============
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ==============
 */

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.util.JsonParserDelegate;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBase;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.std.DelegatingDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.util.NameTransformer;
import com.fasterxml.jackson.dataformat.xml.deser.ElementWrappable;
import com.fasterxml.jackson.dataformat.xml.util.TypeUtil;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Copy of `WrapperHandlingDeserializer` with support for unwrapping.
 */
public class UnwrappableWrapperHandlingDeserializer extends DelegatingDeserializer {
    private static final long serialVersionUID = 1L;

    /**
     * (Simple) Names of properties, for which virtual wrapping is needed
     * to compensate: these are so-called 'unwrapped' XML lists where property
     * name is used for elements, and not as List markers.
     */
    protected final Set<String> _namesToWrap;

    protected final JavaType _type;

    // @since 2.12
    protected final boolean _caseInsensitive;

    public UnwrappableWrapperHandlingDeserializer(BeanDeserializerBase delegate) {
        this(delegate, null);
    }

    public UnwrappableWrapperHandlingDeserializer(BeanDeserializerBase delegate, Set<String> namesToWrap)
    {
        super(delegate);
        _namesToWrap = namesToWrap;
        _type = delegate.getValueType();
        _caseInsensitive = delegate.isCaseInsensitive();
    }

    @Override
    protected JsonDeserializer<?> newDelegatingInstance(JsonDeserializer<?> newDelegatee0) {
        // default not enough, as we may need to create a new wrapping deserializer
        // even if delegatee does not change
        throw new IllegalStateException("Internal error: should never get called");
    }

    @Override
    @SuppressWarnings("unchecked")
    public JsonDeserializer<Object> unwrappingDeserializer(NameTransformer unwrapper) {
        JsonDeserializer<?> unwrapping = _delegatee.unwrappingDeserializer(unwrapper);
        if (unwrapping == _delegatee) {
            return this;
        }
        if (unwrapping instanceof BeanDeserializerBase) {
            return new UnwrappableWrapperHandlingDeserializer((BeanDeserializerBase) unwrapping, _namesToWrap);
        }
        return (JsonDeserializer<Object>) newDelegatingInstance(unwrapping);
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt,
                                                BeanProperty property)
            throws JsonMappingException
    {
        JavaType vt = _type;
        if (vt == null) {
            vt = ctxt.constructType(_delegatee.handledType());
        }
        JsonDeserializer<?> del = ctxt.handleSecondaryContextualization(_delegatee, property, vt);
        BeanDeserializerBase newDelegatee = _verifyDeserType(del);

        // Let's go through the properties now...
        Iterator<SettableBeanProperty> it = newDelegatee.properties();
        HashSet<String> unwrappedNames = null;
        while (it.hasNext()) {
            SettableBeanProperty prop = it.next();
            // First things first: only consider array/Collection types
            // (not perfect check, but simplest reasonable check)
            JavaType type = prop.getType();
            if (!TypeUtil.isIndexedType(type)) {
                continue;
            }
            PropertyName wrapperName = prop.getWrapperName();
            // skip anything with wrapper (should work as is)
            if ((wrapperName != null) && (wrapperName != PropertyName.NO_NAME)) {
                continue;
            }
            if (unwrappedNames == null) {
                unwrappedNames = new HashSet<String>();
            }
            // not optimal; should be able to use PropertyName...
            unwrappedNames.add(prop.getName());
            for (PropertyName alias : prop.findAliases(ctxt.getConfig())) {
                unwrappedNames.add(alias.getSimpleName());
            }
        }
        // Ok: if nothing to take care of, just return the delegatee...
        if (unwrappedNames == null) {
            return newDelegatee;
        }
        // Otherwise, create the thing that can deal with virtual wrapping
        return new UnwrappableWrapperHandlingDeserializer(newDelegatee, unwrappedNames);
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        _configureParser(p);
        return _delegatee.deserialize(p,  ctxt);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt,
                              Object intoValue) throws IOException
    {
        _configureParser(p);
        return ((JsonDeserializer<Object>)_delegatee).deserialize(p, ctxt, intoValue);
    }

    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt,
                                      TypeDeserializer typeDeserializer) throws IOException
    {
        _configureParser(p);
        return _delegatee.deserializeWithType(p, ctxt, typeDeserializer);
    }
    
    /*
    /**********************************************************************
    /* Internal methods
    /**********************************************************************
     */

    protected final void _configureParser(JsonParser p) throws IOException
    {
        // 05-Sep-2019, tatu: May get XML parser, except for case where content is
        //   buffered. In that case we may still have access to real parser if we
        //   are lucky (like in [dataformat-xml#242])
        while (p instanceof JsonParserDelegate) {
            p = ((JsonParserDelegate) p).delegate();
        }
        if ((p instanceof ElementWrappable) && (_namesToWrap != null)) {
            // 03-May-2021, tatu: as per [dataformat-xml#469] there are special
            //   cases where we get String token to represent XML empty element.
            //   If so, need to refrain from adding wrapping as that would
            //   override parent settings
            JsonToken t = p.currentToken();
            if (t == JsonToken.START_OBJECT || t == JsonToken.START_ARRAY
                    // 12-Dec-2021, tatu: [dataformat-xml#490] There seems to be
                    //    cases here (similar to regular JSON) where leading START_OBJECT
                    //    is consumed during buffering, so need to consider that too
                    //    it seems (just hope we are at correct level and not off by one...)
                    || t == JsonToken.FIELD_NAME) {
                ((ElementWrappable) p).addVirtualWrapping(_namesToWrap, _caseInsensitive);
            }
        }
    }

    protected BeanDeserializerBase _verifyDeserType(JsonDeserializer<?> deser)
    {
        if (!(deser instanceof BeanDeserializerBase)) {
            throw new IllegalArgumentException("Can not change delegate to be of type "
                    +deser.getClass().getName());
        }
        return (BeanDeserializerBase) deser;
    }
}
