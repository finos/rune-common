package org.finos.rune.mapper.serializer;

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
import com.rosetta.model.lib.annotations.RuneMetaType;
import com.rosetta.model.lib.meta.FieldWithMeta;
import org.finos.rune.mapper.RuneJsonConfig;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class RuneChoiceTypeSerializer extends JsonSerializer<RosettaModelObject> {

    private static final String DATA = "@data";

    @Override
    public void serialize(RosettaModelObject value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        ChoiceValue choiceValue = getChoiceValue(value);
        Set<String> writtenFields = new HashSet<>();

        gen.writeStartObject();
        writeChoiceMetadata(value, gen, serializers, writtenFields);
        if (choiceValue != null) {
            writeChoiceValue(choiceValue, gen, serializers, writtenFields);
        }
        gen.writeEndObject();
    }

    @Override
    public Class<RosettaModelObject> handledType() {
        return RosettaModelObject.class;
    }

    private void writeChoiceValue(ChoiceValue choiceValue, JsonGenerator gen, SerializerProvider serializers, Set<String> writtenFields) throws IOException {
        Object selectedValue = choiceValue.value;
        validateDeclaredChoiceOptionType(choiceValue, serializers);
        if (selectedValue instanceof RosettaModelObject) {
            RosettaModelObject selectedRosettaValue = (RosettaModelObject) selectedValue;
            if (isChoiceType(selectedRosettaValue.getType())) {
                ChoiceValue nestedChoiceValue = getChoiceValue(selectedRosettaValue);
                if (nestedChoiceValue == null) {
                    throw new IOException("Nested Rune choice has no selected value for " + selectedRosettaValue.getType().getName());
                }
                writeChoiceValue(nestedChoiceValue, gen, serializers, writtenFields);
                return;
            }

            writeStringFieldIfAbsent(RuneJsonConfig.MetaProperties.TYPE, resolveChoiceTypeName(selectedRosettaValue), gen, writtenFields);
            writeRosettaFields(selectedRosettaValue, gen, serializers, writtenFields);
        } else {
            writeStringFieldIfAbsent(RuneJsonConfig.MetaProperties.TYPE, choiceValue.choiceOptionType, gen, writtenFields);
            if (writtenFields.add(DATA)) {
                gen.writeFieldName(DATA);
                serializers.defaultSerializeValue(selectedValue, gen);
            }
        }
    }

    private void writeRosettaFields(RosettaModelObject value, JsonGenerator gen, SerializerProvider serializers, Set<String> writtenFields) throws IOException {
        TokenBuffer buffer = new TokenBuffer(gen.getCodec(), false);
        serializers.defaultSerializeValue(value, buffer);
        JsonNode node = gen.getCodec().readTree(buffer.asParser());

        if (node instanceof ObjectNode) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (!RuneJsonConfig.getMetaProperties().contains(field.getKey()) && writtenFields.add(field.getKey())) {
                    gen.writeFieldName(field.getKey());
                    gen.writeTree(field.getValue());
                }
            }
        }
    }

    private ChoiceValue getChoiceValue(RosettaModelObject value) throws IOException {
        ChoiceValue selected = null;
        for (Method method : value.getClass().getMethods()) {
            RuneAttribute attribute = method.getAnnotation(RuneAttribute.class);
            if (attribute != null
                    && method.getParameterCount() == 0
                    && !RuneJsonConfig.MetaProperties.TYPE.equals(attribute.value())
                    && !method.isAnnotationPresent(RuneMetaType.class)) {
                Object selectedValue = invoke(method, value);
                if (!isEmptyChoiceValue(selectedValue)) {
                    Class<?> choiceType = value.getType();
                    ChoiceValue candidate = new ChoiceValue(
                            choiceType == null ? value.getClass().getName() : choiceType.getName(),
                            attribute.value(),
                            method.getReturnType(),
                            selectedValue
                    );
                    if (selected != null) {
                        // Choice types must have exactly one populated option.
                        throw new IOException(
                                String.format("Multiple Rune choice options selected for %s: %s and %s", value.getType().getName(), selected.choiceOptionType, candidate.choiceOptionType)
                        );
                    }
                    selected = candidate;
                }
            }
        }
        return selected;
    }

    private void writeChoiceMetadata(RosettaModelObject value, JsonGenerator gen, SerializerProvider serializers, Set<String> writtenFields) throws IOException {
        Set<String> serializedMetadataAttributes = new HashSet<>();
        for (Method method : value.getClass().getMethods()) {
            RuneAttribute attribute = method.getAnnotation(RuneAttribute.class);
            if (attribute == null || method.getParameterCount() != 0 || !method.isAnnotationPresent(RuneMetaType.class)) {
                continue;
            }
            if (!serializedMetadataAttributes.add(attribute.value())) {
                continue;
            }

            Object metadataValue = invoke(method, value);
            if (metadataValue == null) {
                continue;
            }

            writeUnwrappedObjectFields(metadataValue, gen, serializers, writtenFields);
        }
    }

    private void writeUnwrappedObjectFields(Object value, JsonGenerator gen, SerializerProvider serializers, Set<String> writtenFields) throws IOException {
        TokenBuffer buffer = new TokenBuffer(gen.getCodec(), false);
        serializers.defaultSerializeValue(value, buffer);
        JsonNode node = gen.getCodec().readTree(buffer.asParser());

        if (node instanceof ObjectNode) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (RuneJsonConfig.getMetaProperties().contains(field.getKey())
                        || !writtenFields.add(field.getKey())) {
                    continue;
                }
                gen.writeFieldName(field.getKey());
                gen.writeTree(field.getValue());
            }
        }
    }

    private void writeStringFieldIfAbsent(String fieldName, String value, JsonGenerator gen, Set<String> writtenFields) throws IOException {
        if (writtenFields.add(fieldName)) {
            gen.writeStringField(fieldName, value);
        }
    }

    private Object invoke(Method method, Object target) throws IOException {
        try {
            return method.invoke(target);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IOException("Unable to inspect Rune choice type", e);
        }
    }

    private void validateDeclaredChoiceOptionType(ChoiceValue choiceValue, SerializerProvider serializers) throws IOException {
        if (!(choiceValue.value instanceof RosettaModelObject)) {
            return;
        }

        RosettaModelObject selectedRosettaValue = (RosettaModelObject) choiceValue.value;
        Class<?> actualType = selectedRosettaValue.getType();

        if (actualType != choiceValue.declaredType) {
            serializers.reportMappingProblem(
                    "Unable to serialize Rune choice option '%s' for choice type '%s': declared type is '%s' but runtime type is '%s'. "
                            + "The runtime type must exactly match the declared choice option type.",
                    choiceValue.choiceOptionType,
                    choiceValue.choiceType,
                    choiceValue.declaredType.getName(),
                    actualType.getName()
            );
        }
    }

    private boolean isEmptyChoiceValue(Object value) {
        return value == null || value instanceof Iterable && !((Iterable<?>) value).iterator().hasNext();
    }

    private boolean isChoiceType(Class<?> type) {
        return type != null && type.isAnnotationPresent(RuneChoiceType.class);
    }

    private String resolveChoiceTypeName(RosettaModelObject selectedValue) throws IOException {
        Class<?> selectedType = selectedValue.getType();
        if (selectedType == null) {
            throw new IOException("Unable to resolve Rune choice value type");
        }

        Class<?> metaWrappedType = findMetaWrappedType(selectedValue);
        if (metaWrappedType != null) {
            return metaWrappedType.getName();
        }

        return selectedType.getName();
    }

    private Class<?> findMetaWrappedType(RosettaModelObject selectedValue) {
        if (selectedValue instanceof FieldWithMeta) {
            return ((FieldWithMeta<?>) selectedValue).getValueType();
        }
        return null;
    }

    private static class ChoiceValue {
        private final String choiceType;
        private final String choiceOptionType;
        private final Class<?> declaredType;
        private final Object value;

        private ChoiceValue(String choiceType, String choiceOptionType, Class<?> declaredType, Object value) {
            this.choiceType = choiceType;
            this.choiceOptionType = choiceOptionType;
            this.declaredType = declaredType;
            this.value = value;
        }
    }
}
