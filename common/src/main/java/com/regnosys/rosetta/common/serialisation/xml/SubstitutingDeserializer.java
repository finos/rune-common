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

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SubstitutingDeserializer extends StdDeserializer<Object>
        implements ContextualDeserializer,
        ValueInstantiator.Gettable {

    private final Map<String, JsonDeserializer<Object>> _substitutedDeserializers;

    protected SubstitutingDeserializer(Class<?> vc, Map<String, JsonDeserializer<Object>> substitutedDeserializers) {
        super(vc);
        this._substitutedDeserializers = substitutedDeserializers;
    }

    protected SubstitutingDeserializer(JavaType valueType, Map<String, JsonDeserializer<Object>> substitutedDeserializers) {
        super(valueType);
        this._substitutedDeserializers = substitutedDeserializers;
    }

    protected SubstitutingDeserializer(SubstitutingDeserializer src, Map<String, JsonDeserializer<Object>> substitutedDeserializers) {
        super(src);
        this._substitutedDeserializers = substitutedDeserializers;
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        if (p.isExpectedStartObjectToken()) {
            String substitutedName = p.currentName();
            JsonDeserializer<Object> valueDes = _substitutedDeserializers.get(substitutedName);
            if (valueDes == null) {
                return null;
            }
            Object value;
            p.nextToken();
            value = valueDes.deserialize(p, ctxt);
            p.nextToken();
            p.nextToken();
            p.nextToken();
            return value;
        }
        return ctxt.handleUnexpectedToken(getValueType(ctxt), p);
    }

    @Override
    @SuppressWarnings("unchecked")
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
        SubstitutingMethodProperty subProp = (SubstitutingMethodProperty)property;
        SubstitutionMap map = subProp.getSubstitutionMap();

        Map<String, JsonDeserializer<Object>> subDesers;
        if (_substitutedDeserializers == null) {
            subDesers = new HashMap<>();
        } else {
            subDesers = new HashMap<>(_substitutedDeserializers);
        }
        for (JavaType vt : map.getTypes()) {
            String substitutedName = map.getName(vt);
            JsonDeserializer<?> valueDeser = subDesers.get(substitutedName);
            valueDeser = findConvertingContentDeserializer(ctxt, property, valueDeser);
            if (valueDeser == null) {
                valueDeser = ctxt.findContextualValueDeserializer(vt, property);
            } else { // if directly assigned, probably not yet contextual, so:
                valueDeser = ctxt.handleSecondaryContextualization(valueDeser, property, vt);
            }
            subDesers.put(substitutedName, (JsonDeserializer<Object>) valueDeser);
        }
        if (!subDesers.equals(_substitutedDeserializers)) {
            return new SubstitutingDeserializer(this, subDesers);
        }
        return this;
    }
}
