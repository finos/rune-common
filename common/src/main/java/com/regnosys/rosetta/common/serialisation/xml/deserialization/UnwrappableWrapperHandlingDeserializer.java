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

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBase;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.util.NameTransformer;
import com.fasterxml.jackson.dataformat.xml.deser.WrapperHandlingDeserializer;
import com.fasterxml.jackson.dataformat.xml.util.TypeUtil;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * An extension of `WrapperHandlingDeserializer` with support for unwrapping.
 */
public class UnwrappableWrapperHandlingDeserializer extends WrapperHandlingDeserializer {

    public UnwrappableWrapperHandlingDeserializer(BeanDeserializerBase delegate) {
        super(delegate);
    }

    public UnwrappableWrapperHandlingDeserializer(BeanDeserializerBase delegate, Set<String> namesToWrap)
    {
        super(delegate, namesToWrap);
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
}
