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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneChoiceType;
import org.finos.rune.mapper.RuneJsonConfig;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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

        String typeId = getTypeId(node, ctxt, p);
        Object builder = newBuilder();

        for (Method setter : builder.getClass().getMethods()) {
            RuneAttribute attribute = setter.getAnnotation(RuneAttribute.class);
            if (attribute == null || setter.getParameterCount() != 1 || RuneJsonConfig.MetaProperties.TYPE.equals(attribute.value())) {
                continue;
            }

            Object value = resolveValue(attribute.value(), setter.getParameterTypes()[0], typeId, (ObjectNode) node, mapper);
            if (value != null) {
                invoke(setter, builder, value);
                return build(builder);
            }
        }

        throw ctxt.weirdStringException(typeId, choiceType, "Unable to resolve Rune choice option");
    }

    private Object resolveValue(String optionName, Class<?> optionType, String typeId, ObjectNode node, ObjectMapper mapper) throws IOException {
        if (RosettaModelObject.class.isAssignableFrom(optionType)) {
            if (optionType.isAnnotationPresent(RuneChoiceType.class)) {
                try {
                    RosettaModelObject nestedChoice = mapper.treeToValue(node, optionType.asSubclass(RosettaModelObject.class));
                    return hasChoiceValue(nestedChoice) ? nestedChoice : null;
                } catch (IOException e) {
                    return null;
                }
            }

            if (typeId.equals(optionType.getCanonicalName()) || typeId.equals(optionType.getName())) {
                return mapper.treeToValue(withoutType(node), optionType);
            }
            return null;
        }

        if (typeId.equals(optionName)) {
            JsonNode data = node.get(DATA);
            return data == null || data.isNull() ? null : mapper.treeToValue(data, optionType);
        }
        return null;
    }

    private ObjectNode withoutType(ObjectNode node) {
        ObjectNode copy = node.deepCopy();
        copy.remove(RuneJsonConfig.MetaProperties.TYPE);
        return copy;
    }

    private boolean hasChoiceValue(RosettaModelObject choice) throws IOException {
        for (Method method : choice.getClass().getMethods()) {
            RuneAttribute attribute = method.getAnnotation(RuneAttribute.class);
            if (attribute != null && method.getParameterCount() == 0 && !RuneJsonConfig.MetaProperties.TYPE.equals(attribute.value())) {
                Object value = invoke(method, choice);
                if (value != null) {
                    return true;
                }
            }
        }
        return false;
    }

    private String getTypeId(JsonNode node, DeserializationContext ctxt, JsonParser p) throws IOException {
        JsonNode type = node.get(RuneJsonConfig.MetaProperties.TYPE);
        if (type == null || type.isNull()) {
            return (String) ctxt.handleUnexpectedToken(String.class, p);
        }
        return type.asText();
    }

    private Object newBuilder() throws IOException {
        try {
            return choiceType.getMethod("builder").invoke(null);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IOException("Unable to create Rune choice type builder for " + choiceType.getName(), e);
        }
    }

    private RosettaModelObject build(Object builder) throws IOException {
        try {
            return (RosettaModelObject) builder.getClass().getMethod("build").invoke(builder);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IOException("Unable to build Rune choice type " + choiceType.getName(), e);
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
