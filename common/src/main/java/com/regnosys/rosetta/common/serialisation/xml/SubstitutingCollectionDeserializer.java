package com.regnosys.rosetta.common.serialisation.xml;

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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.NullValueProvider;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.deser.std.CollectionDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.util.ClassUtil;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SubstitutingCollectionDeserializer extends CollectionDeserializer {

    private final Map<String, JsonDeserializer<Object>> _valueDeserializers;

    protected SubstitutingCollectionDeserializer(JavaType collectionType, Map<String, JsonDeserializer<Object>> valueDeserializers, TypeDeserializer valueTypeDeser, ValueInstantiator valueInstantiator, JsonDeserializer<Object> delegateDeser, NullValueProvider nuller, Boolean unwrapSingle) {
        super(collectionType, null, valueTypeDeser, valueInstantiator, delegateDeser, nuller, unwrapSingle);
        this._valueDeserializers = valueDeserializers;
    }

    protected SubstitutingCollectionDeserializer(CollectionDeserializer src, Map<String, JsonDeserializer<Object>> valueDeserializers) {
        super(src);
        this._valueDeserializers = valueDeserializers;
    }

    @SuppressWarnings("unchecked")
    protected SubstitutingCollectionDeserializer withResolved(JsonDeserializer<?> dd,
                                                  Map<String, JsonDeserializer<Object>> valueDeserializers, TypeDeserializer vtd,
                                                  NullValueProvider nuller, Boolean unwrapSingle)
    {
        return new SubstitutingCollectionDeserializer(_containerType,
                valueDeserializers, vtd,
                _valueInstantiator, (JsonDeserializer<Object>) dd,
                nuller, unwrapSingle);
    }

    @Override
    @SuppressWarnings("unchecked")
    public SubstitutingCollectionDeserializer createContextual(DeserializationContext ctxt,
                                                   BeanProperty property) throws JsonMappingException
    {
        // May need to resolve types for delegate-based creators:
        JsonDeserializer<Object> delegateDeser = null;
        if (_valueInstantiator != null) {
            if (_valueInstantiator.canCreateUsingDelegate()) {
                JavaType delegateType = _valueInstantiator.getDelegateType(ctxt.getConfig());
                if (delegateType == null) {
                    ctxt.reportBadDefinition(_containerType, String.format(
                            "Invalid delegate-creator definition for %s: value instantiator (%s) returned true for 'canCreateUsingDelegate()', but null for 'getDelegateType()'",
                            _containerType,
                            _valueInstantiator.getClass().getName()));
                }
                delegateDeser = findDeserializer(ctxt, delegateType, property);
            } else if (_valueInstantiator.canCreateUsingArrayDelegate()) {
                JavaType delegateType = _valueInstantiator.getArrayDelegateType(ctxt.getConfig());
                if (delegateType == null) {
                    ctxt.reportBadDefinition(_containerType, String.format(
                            "Invalid delegate-creator definition for %s: value instantiator (%s) returned true for 'canCreateUsingArrayDelegate()', but null for 'getArrayDelegateType()'",
                            _containerType,
                            _valueInstantiator.getClass().getName()));
                }
                delegateDeser = findDeserializer(ctxt, delegateType, property);
            }
        }
        // [databind#1043]: allow per-property allow-wrapping of single overrides:
        // 11-Dec-2015, tatu: Should we pass basic `Collection.class`, or more refined? Mostly
        //   comes down to "List vs Collection" I suppose... for now, pass Collection
        Boolean unwrapSingle = findFormatFeature(ctxt, property, Collection.class,
                JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

        SubstitutingMethodProperty subProp = (SubstitutingMethodProperty)property;
        SubstitutionMap map = subProp.getSubstitutionMap();

        Map<String, JsonDeserializer<Object>> valueDesers;
        if (_valueDeserializers == null) {
            valueDesers = new HashMap<>();
        } else {
            valueDesers = new HashMap<>(_valueDeserializers);
        }
        JsonDeserializer<?> last = null;
        for (JavaType vt : map.getTypes()) {
            String substitutedName = map.getName(vt);
            JsonDeserializer<?> valueDeser = valueDesers.get(substitutedName);
            valueDeser = findConvertingContentDeserializer(ctxt, property, valueDeser);
            if (valueDeser == null) {
                valueDeser = ctxt.findContextualValueDeserializer(vt, property);
            } else { // if directly assigned, probably not yet contextual, so:
                valueDeser = ctxt.handleSecondaryContextualization(valueDeser, property, vt);
            }
            valueDesers.put(substitutedName, (JsonDeserializer<Object>) valueDeser);
            last = valueDeser;
        }

        // and finally, type deserializer needs context as well
        TypeDeserializer valueTypeDeser = _valueTypeDeserializer;
        if (valueTypeDeser != null) {
            valueTypeDeser = valueTypeDeser.forProperty(property);
        }
        NullValueProvider nuller = findContentNullProvider(ctxt, property, last);
        if ((!Objects.equals(unwrapSingle, _unwrapSingle))
                || (nuller != _nullProvider)
                || (delegateDeser != _delegateDeserializer)
                || (!valueDesers.equals(_valueDeserializers))
                || (valueTypeDeser != _valueTypeDeserializer)
        ) {
            return withResolved(delegateDeser, valueDesers, valueTypeDeser,
                    nuller, unwrapSingle);
        }
        return this;
    }

    @Override
    public Collection<Object> deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException
    {
        if (_delegateDeserializer != null) {
            return (Collection<Object>) _valueInstantiator.createUsingDelegate(ctxt,
                    _delegateDeserializer.deserialize(p, ctxt));
        }
        if (p.getCurrentToken() == JsonToken.START_OBJECT) {
            return _deserializeFromArray(p, ctxt, createDefaultInstance(ctxt));
        }
        if (p.hasToken(JsonToken.VALUE_STRING)) {
            return _deserializeFromString(p, ctxt, p.getText());
        }
        return handleNonArray(p, ctxt, createDefaultInstance(ctxt));
    }

    @Override
    protected Collection<Object> _deserializeFromArray(JsonParser p, DeserializationContext ctxt,
                                                       Collection<Object> result)
            throws IOException
    {
        p.assignCurrentValue(result);

        JsonToken t;
        while ((t = p.nextToken()) == JsonToken.FIELD_NAME) {
            String substitutedName = p.currentName();
            JsonDeserializer<Object> valueDes = _valueDeserializers.get(substitutedName);
            if (valueDes == null) {
                return result;
            }
            try {
                Object value;
                if (t == JsonToken.VALUE_NULL) {
                    if (_skipNullValues) {
                        continue;
                    }
                    value = _nullProvider.getNullValue(ctxt);
                } else {
                    p.nextToken();
                    value = valueDes.deserialize(p, ctxt);
                    p.nextToken();
                }
                p.nextToken();
                p.nextToken();
                if (value == null) {
                    _tryToAddNull(p, ctxt, result);
                    continue;
                }
                result.add(value);
            } catch (Exception e) {
                boolean wrap = (ctxt == null) || ctxt.isEnabled(DeserializationFeature.WRAP_EXCEPTIONS);
                if (!wrap) {
                    ClassUtil.throwIfRTE(e);
                }
                throw JsonMappingException.wrapWithPath(e, result, result.size());
            }
        }
        return result;
    }
}
