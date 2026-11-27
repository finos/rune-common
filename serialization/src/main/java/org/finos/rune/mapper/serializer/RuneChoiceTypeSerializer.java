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
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneChoiceType;
import org.finos.rune.mapper.RuneJsonConfig;
import org.finos.rune.mapper.attributesonly.AttributesOnlySerializerProvider;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class RuneChoiceTypeSerializer extends JsonSerializer<RosettaModelObject> {

    @Override
    public void serialize(RosettaModelObject value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        ChoiceValue choiceValue = getChoiceValue(value);

        gen.writeStartObject();
        if (choiceValue != null) {
            writeChoiceValue(RuneChoiceOptionRegistry.of(value.getType()), Collections.emptyList(), choiceValue, gen, serializers);
        }
        gen.writeEndObject();
    }

    @Override
    public Class<RosettaModelObject> handledType() {
        return RosettaModelObject.class;
    }

    private void writeChoiceValue(RuneChoiceOptionRegistry options, List<String> enclosingOptionNames, ChoiceValue choiceValue,
                                  JsonGenerator gen, SerializerProvider serializers) throws IOException {
        List<String> optionNames = RuneChoiceOptionRegistry.append(enclosingOptionNames, choiceValue.choiceOptionName);
        Object selectedValue = choiceValue.value;
        validateDeclaredChoiceOptionType(choiceValue, serializers);
        RosettaModelObject selectedRosettaValue = selectedValue instanceof RosettaModelObject ? (RosettaModelObject) selectedValue : null;
        if (selectedRosettaValue != null && isChoiceType(selectedRosettaValue.getType())) {
            ChoiceValue nestedChoiceValue = getChoiceValue(selectedRosettaValue);
            if (nestedChoiceValue == null) {
                throw new IOException("Nested Rune choice has no selected value for " + selectedRosettaValue.getType().getName());
            }
            writeChoiceValue(options, optionNames, nestedChoiceValue, gen, serializers);
            return;
        }

        gen.writeStringField(RuneJsonConfig.MetaProperties.TYPE, resolveTypeName(options, optionNames, serializers));
        if (selectedRosettaValue != null) {
            writeRosettaFields(selectedRosettaValue, gen, serializers);
        } else {
            gen.writeFieldName(RuneJsonConfig.MetaProperties.DATA);
            serializers.defaultSerializeValue(selectedValue, gen);
        }
    }

    /** Attributes-only output drops {@code @type} and is never read back, so an ambiguous one does not matter there. */
    private String resolveTypeName(RuneChoiceOptionRegistry options, List<String> optionNames, SerializerProvider serializers) throws IOException {
        String typeName = options.resolveTypeName(optionNames);
        String ambiguity = options.ambiguity(typeName);
        if (ambiguity != null && !(serializers instanceof AttributesOnlySerializerProvider)) {
            throw new IOException(ambiguity);
        }
        return typeName;
    }

    private void writeRosettaFields(RosettaModelObject value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        TokenBuffer buffer = new TokenBuffer(gen.getCodec(), false);
        serializers.defaultSerializeValue(value, buffer);
        JsonNode node = gen.getCodec().readTree(buffer.asParser());

        if (node instanceof ObjectNode) {
            for (Map.Entry<String, JsonNode> field : node.properties()) {
                if (!RuneJsonConfig.getMetaProperties().contains(field.getKey())) {
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
            if (method.getParameterCount() == 0
                    // When serializing a builder, each choice option is exposed twice: by the
                    // covariant builder getter (e.g. A.ABuilder getA()) and by the synthetic
                    // bridge that satisfies the base interface (A getA()). Ignore the builder-typed
                    // getter and keep the base-typed accessor, so a single option counts once and
                    // declaredType matches the runtime built type in validateDeclaredChoiceOptionType.
                    && !RosettaModelObjectBuilder.class.isAssignableFrom(method.getReturnType())
                    && RuneChoiceOptionRegistry.isOption(attribute, method.getReturnType())) {
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
                                String.format("Multiple Rune choice options selected for %s: %s and %s", value.getType().getName(), selected.choiceOptionName, candidate.choiceOptionName)
                        );
                    }
                    selected = candidate;
                }
            }
        }
        return selected;
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
                    choiceValue.choiceOptionName,
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

    private static class ChoiceValue {
        private final String choiceType;
        private final String choiceOptionName;
        private final Class<?> declaredType;
        private final Object value;

        private ChoiceValue(String choiceType, String choiceOptionName, Class<?> declaredType, Object value) {
            this.choiceType = choiceType;
            this.choiceOptionName = choiceOptionName;
            this.declaredType = declaredType;
            this.value = value;
        }
    }
}
