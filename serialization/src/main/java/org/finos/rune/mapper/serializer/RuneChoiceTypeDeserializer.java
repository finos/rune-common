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
import org.finos.rune.mapper.RuneJsonConfig;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class RuneChoiceTypeDeserializer extends JsonDeserializer<RosettaModelObject> {

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
        RuneChoiceOptionRegistry options = RuneChoiceOptionRegistry.of(choiceType);
        RuneChoiceOptionRegistry.Option option;
        try {
            option = options.findByTypeName(runeType);
        } catch (IOException e) {
            ctxt.reportInputMismatch(choiceType, "%s", e.getMessage());
            return null;
        }
        Object value = option == null ? null : readOptionValue(option, (ObjectNode) node, mapper);
        if (value == null) {
            ctxt.reportInputMismatch(
                    choiceType,
                    "Unable to resolve Rune choice option '%s' for choice type '%s'. "
                            + "The @type value must exactly match one of the declared choice options.",
                    runeType,
                    choiceType.getName()
            );
            return null;
        }
        return (RosettaModelObject) wrapInEnclosingChoices(option, value);
    }

    private Object readOptionValue(RuneChoiceOptionRegistry.Option option, ObjectNode node, ObjectMapper mapper) throws IOException {
        if (option.metaWrapped) {
            ObjectNode metaWrapperNode = node.deepCopy();
            metaWrapperNode.remove(RuneJsonConfig.MetaProperties.TYPE);
            return mapper.treeToValue(metaWrapperNode, option.type);
        }
        if (RosettaModelObject.class.isAssignableFrom(option.type)) {
            return mapper.treeToValue(node, option.type);
        }
        JsonNode data = node.get(RuneJsonConfig.MetaProperties.DATA);
        return data == null || data.isNull() ? null : mapper.treeToValue(data, option.type);
    }

    /** Wraps {@code value} in a built instance of each enclosing choice, innermost first, and returns the outermost. */
    private Object wrapInEnclosingChoices(RuneChoiceOptionRegistry.Option option, Object value) throws IOException {
        Object result = value;
        for (int i = option.setters.size() - 1; i >= 0; i--) {
            Class<?> enclosingChoiceType = i == 0 ? choiceType : option.setters.get(i - 1).getParameterTypes()[0];
            RosettaModelObjectBuilder builder = RuneChoiceOptionRegistry.newBuilder(enclosingChoiceType);
            invoke(option.setters.get(i), builder, result);
            result = builder.build();
        }
        return result;
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

    private Object invoke(Method method, Object target, Object... args) throws IOException {
        try {
            return method.invoke(target, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IOException("Unable to inspect Rune choice type", e);
        }
    }
}
