package org.finos.rune.mapper.introspector;

/*-
 * ==============
 * Rune Common
 * ==============
 * Copyright (C) 2018 - 2026 REGnosys
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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneChoiceType;
import org.finos.rune.mapper.RuneJsonConfig;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;

public class RuneChoiceTypeSerializer extends JsonSerializer<RosettaModelObject> {

    private static final String DATA = "@data";

    @Override
    public void serialize(RosettaModelObject value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        ChoiceValue choiceValue = getChoiceValue(value);

        gen.writeStartObject();
        if (choiceValue != null) {
            writeChoiceValue(choiceValue, gen, serializers);
        }
        gen.writeEndObject();
    }

    @Override
    public Class<RosettaModelObject> handledType() {
        return RosettaModelObject.class;
    }

    private void writeChoiceValue(ChoiceValue choiceValue, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        Object selectedValue = choiceValue.value;
        if (selectedValue instanceof RosettaModelObject) {
            RosettaModelObject selectedRosettaValue = (RosettaModelObject) selectedValue;
            if (isChoiceType(selectedRosettaValue.getType())) {
                ChoiceValue nestedChoiceValue = getChoiceValue(selectedRosettaValue);
                if (nestedChoiceValue != null) {
                    writeChoiceValue(nestedChoiceValue, gen, serializers);
                }
                return;
            }

            gen.writeStringField(RuneJsonConfig.MetaProperties.TYPE, selectedRosettaValue.getType().getCanonicalName());
            writeRosettaFields(selectedRosettaValue, gen, serializers);
        } else {
            gen.writeStringField(RuneJsonConfig.MetaProperties.TYPE, choiceValue.name);
            gen.writeFieldName(DATA);
            serializers.defaultSerializeValue(selectedValue, gen);
        }
    }

    private void writeRosettaFields(RosettaModelObject value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        TokenBuffer buffer = new TokenBuffer(gen.getCodec(), false);
        serializers.defaultSerializeValue(value, buffer);
        JsonNode node = gen.getCodec().readTree(buffer.asParser());

        if (node instanceof ObjectNode) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (!RuneJsonConfig.getMetaProperties().contains(field.getKey())) {
                    gen.writeFieldName(field.getKey());
                    gen.writeTree(field.getValue());
                }
            }
        }
    }

    private ChoiceValue getChoiceValue(RosettaModelObject value) throws IOException {
        for (Method method : value.getClass().getMethods()) {
            RuneAttribute attribute = method.getAnnotation(RuneAttribute.class);
            if (attribute != null && method.getParameterCount() == 0 && !RuneJsonConfig.MetaProperties.TYPE.equals(attribute.value())) {
                Object selectedValue = invoke(method, value);
                if (!isEmptyChoiceValue(selectedValue)) {
                    return new ChoiceValue(attribute.value(), selectedValue);
                }
            }
        }
        return null;
    }

    private Object invoke(Method method, Object target) throws IOException {
        try {
            return method.invoke(target);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IOException("Unable to inspect Rune choice type", e);
        }
    }

    private boolean isEmptyChoiceValue(Object value) {
        return value == null || value instanceof Iterable && !((Iterable<?>) value).iterator().hasNext();
    }

    private boolean isChoiceType(Class<?> type) {
        return type != null && type.isAnnotationPresent(RuneChoiceType.class);
    }

    private static class ChoiceValue {
        private final String name;
        private final Object value;

        private ChoiceValue(String name, Object value) {
            this.name = name;
            this.value = value;
        }
    }
}
