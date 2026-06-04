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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneChoiceType;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.annotations.RuneMetaType;
import com.rosetta.model.lib.meta.FieldWithMeta;
import org.finos.rune.mapper.RuneJsonConfig;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;

public class RuneChoiceTypeDeserializer extends JsonDeserializer<RosettaModelObject> {

    private static final String DATA = "@data";

    private final Class<?> choiceType;

    public RuneChoiceTypeDeserializer(Class<?> choiceType) {
        this.choiceType = choiceType;
    }

    @Override
    public RosettaModelObject deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        JsonNode node = mapper.readTree(p);
        if (!(node instanceof ObjectNode)) {
            return (RosettaModelObject) ctxt.handleUnexpectedToken(choiceType, p);
        }

        String runeType = getRuneType(node, ctxt);
        RosettaModelObject result = tryDeserialize((ObjectNode) node, runeType, choiceType, mapper);
        if (result != null) {
            return result;
        }
        ctxt.reportInputMismatch(
                choiceType,
                "Unable to resolve Rune choice option '%s' for choice type '%s'. "
                        + "The @type value must exactly match one of the declared choice options.",
                runeType,
                choiceType.getName()
        );
        return null;
    }

    private RosettaModelObject tryDeserialize(ObjectNode node, String runeType, Class<?> targetChoiceType, ObjectMapper mapper) throws IOException {
        RosettaModelObjectBuilder builder = newBuilder(targetChoiceType);
        boolean selectedOptionResolved = false;
        for (Method setter : builder.getClass().getMethods()) {
            RuneAttribute attribute = setter.getAnnotation(RuneAttribute.class);
            if (attribute == null || setter.getParameterCount() != 1) {
                continue;
            }

            if (setter.isAnnotationPresent(RuneMetaType.class)) {
                Object metadata = resolveChoiceMetadata(setter.getParameterTypes()[0], node, mapper);
                if (metadata != null) {
                    invoke(setter, builder, metadata);
                }
                continue;
            }

            if (RuneJsonConfig.MetaProperties.TYPE.equals(attribute.value()) || selectedOptionResolved) {
                continue;
            }

            Object value = resolveValue(attribute.value(), setter.getParameterTypes()[0], runeType, node, mapper);
            if (value != null) {
                invoke(setter, builder, value);
                selectedOptionResolved = true;
            }
        }
        return selectedOptionResolved ? builder.build() : null;
    }

    private Object resolveChoiceMetadata(Class<?> metadataType, ObjectNode node, ObjectMapper mapper) throws IOException {
        ObjectNode metadataNode = mapper.createObjectNode();
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String fieldName = field.getKey();
            if (fieldName.startsWith("@") && !RuneJsonConfig.MetaProperties.TYPE.equals(fieldName)) {
                metadataNode.set(fieldName, field.getValue());
            }
        }
        if (metadataNode.isEmpty()) {
            return null;
        }
        return mapper.treeToValue(metadataNode, metadataType);
    }

    private Object resolveValue(String optionName, Class<?> optionType, String runeType, ObjectNode node, ObjectMapper mapper) throws IOException {
        if (RosettaModelObject.class.isAssignableFrom(optionType)) {
            if (optionType.isAnnotationPresent(RuneChoiceType.class)) {
                return tryDeserialize(node, runeType, optionType, mapper);
            }

            if (isExactRuneType(optionType, runeType)) {
                return mapper.treeToValue(node, optionType);
            }
            if (isMetaWrapperForType(optionType, runeType)) {
                ObjectNode metaWrapperNode = node.deepCopy();
                metaWrapperNode.remove(RuneJsonConfig.MetaProperties.TYPE);
                return mapper.treeToValue(metaWrapperNode, optionType);
            }
            return null;
        }

        if (runeType.equals(optionName)) {
            JsonNode data = node.get(DATA);
            return data == null || data.isNull() ? null : mapper.treeToValue(data, optionType);
        }
        return null;
    }

    private boolean isExactRuneType(Class<?> optionType, String runeType) {
        return runeType.equals(optionType.getName());
    }

    private boolean isMetaWrapperForType(Class<?> optionType, String runeType) throws IOException {
        if (!FieldWithMeta.class.isAssignableFrom(optionType)) {
            return false;
        }
        Class<?> valueType = ((FieldWithMeta<?>) newBuilder(optionType)).getValueType();
        return valueType != null && runeType.equals(valueType.getName());
    }

    private String getRuneType(JsonNode node, DeserializationContext ctxt) throws IOException {
        JsonNode type = node.get(RuneJsonConfig.MetaProperties.TYPE);
        if (type == null || type.isNull()) {
            ctxt.reportInputMismatch(choiceType, "Missing required '%s' metadata property", RuneJsonConfig.MetaProperties.TYPE);
            return null;
        }
        if (!type.isTextual()) {
            ctxt.reportInputMismatch(choiceType, "'%s' metadata property must be a string", RuneJsonConfig.MetaProperties.TYPE);
            return null;
        }
        return type.asText();
    }

    private RosettaModelObjectBuilder newBuilder(Class<?> choiceType) throws IOException {
        RuneDataType runeDataType = choiceType.getAnnotation(RuneDataType.class);
        if (runeDataType == null) {
            throw new IOException("Unable to find Rune data type metadata for " + choiceType.getName());
        }
        try {
            return runeDataType.builder().getDeclaredConstructor().newInstance();
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
            throw new IOException("Unable to create Rune choice type builder for " + choiceType.getName(), e);
        }
    }

    private Object invoke(Method method, Object target, Object... args) throws IOException {
        try {
            return method.invoke(target, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IOException("Unable to inspect Rune choice type", e);
        }
    }
}
