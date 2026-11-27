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

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneChoiceType;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.FieldWithMeta;
import com.rosetta.model.lib.meta.GlobalKeyFields;
import org.finos.rune.mapper.RuneJsonConfig;
import org.finos.rune.mapper.RuneTypeNameConverter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The options of one Rune choice type, with the options of nested choices flattened in, each with the
 * {@code @type} it is written with. {@link RuneChoiceTypeSerializer} and {@link RuneChoiceTypeDeserializer}
 * both name options here, so the name written is the name read.
 * <p>
 * A data type option is named by its Rune type. Any other option, such as a basic type or an enum, with or
 * without metadata, is named by the option itself. A plain option and a metadata-wrapped option of the
 * same type, one of them in a nested choice, therefore share an {@code @type}: such an {@code @type} is
 * ambiguous, and neither option can be written as JSON that is read back, nor read.
 */
final class RuneChoiceOptionRegistry {

    private static final ClassValue<RuneChoiceOptionRegistry> CACHE = new ClassValue<RuneChoiceOptionRegistry>() {
        @Override
        protected RuneChoiceOptionRegistry computeValue(Class<?> choiceType) {
            try {
                return new RuneChoiceOptionRegistry(choiceType);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    };

    private final Class<?> choiceType;
    private final Map<List<String>, Option> optionsByPath = new LinkedHashMap<>();
    private final Map<String, Option> optionsByTypeName = new LinkedHashMap<>();
    private final Map<String, String> ambiguousTypeNames = new HashMap<>();

    static RuneChoiceOptionRegistry of(Class<?> choiceType) throws IOException {
        try {
            return CACHE.get(choiceType);
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private RuneChoiceOptionRegistry(Class<?> choiceType) throws IOException {
        this.choiceType = choiceType;
        collectOptions(choiceType, Collections.emptyList(), Collections.emptyList());
    }

    /**
     * The {@code @type} of the option reached through the option names in {@code path}, outermost first,
     * whether or not it is ambiguous.
     *
     * @throws IOException when no option has that path
     */
    String resolveTypeName(List<String> path) throws IOException {
        Option option = optionsByPath.get(path);
        if (option == null) {
            throw new IOException("Unknown Rune choice option " + String.join(".", path) + " for " + choiceType.getName());
        }
        return option.typeName;
    }

    /**
     * The option written with {@code typeName}, or {@code null} when no option has it.
     *
     * @throws IOException when two options share {@code typeName}
     */
    Option findByTypeName(String typeName) throws IOException {
        failIfAmbiguous(typeName);
        return optionsByTypeName.get(typeName);
    }

    /** @throws IOException when two options share {@code typeName}, naming both */
    void failIfAmbiguous(String typeName) throws IOException {
        String ambiguity = ambiguousTypeNames.get(typeName);
        if (ambiguity != null) {
            throw new IOException(ambiguity);
        }
    }

    private void collectOptions(Class<?> currentChoiceType, List<Method> setters, List<String> path) throws IOException {
        for (Method setter : newBuilder(currentChoiceType).getClass().getMethods()) {
            RuneAttribute attribute = setter.getAnnotation(RuneAttribute.class);
            if (attribute == null
                    || setter.getParameterCount() != 1
                    || RuneJsonConfig.getMetaProperties().contains(attribute.value())
                    || GlobalKeyFields.class.isAssignableFrom(setter.getParameterTypes()[0])) {
                continue;
            }
            List<Method> optionSetters = append(setters, setter);
            List<String> optionPath = append(path, attribute.value());
            Class<?> optionType = setter.getParameterTypes()[0];
            if (optionType.isAnnotationPresent(RuneChoiceType.class)) {
                collectOptions(optionType, optionSetters, optionPath);
            } else {
                register(newOption(optionSetters, optionPath, optionType));
            }
        }
    }

    private Option newOption(List<Method> setters, List<String> path, Class<?> optionType) throws IOException {
        String optionName = path.get(path.size() - 1);
        boolean metaWrapped = FieldWithMeta.class.isAssignableFrom(optionType);
        Class<?> namedType = metaWrapped ? ((FieldWithMeta<?>) newBuilder(optionType)).getValueType() : optionType;
        boolean namedByOption = namedType == null || !RosettaModelObject.class.isAssignableFrom(namedType);
        String typeName = namedByOption ? optionName : RuneTypeNameConverter.toRuneName(namedType);
        return new Option(setters, path, optionType, typeName, metaWrapped);
    }

    private void register(Option option) {
        optionsByPath.put(option.path, option);
        Option existing = optionsByTypeName.putIfAbsent(option.typeName, option);
        // Two plain or two metadata-wrapped options sharing an @type, reachable only through two nested
        // choices, which rune-dsl warns about, resolve to the first one.
        if (existing != null && existing.metaWrapped != option.metaWrapped) {
            // Option order follows Class.getMethods(), which is unspecified, so the message orders the paths itself.
            List<String> paths = new ArrayList<>(Arrays.asList(String.join(".", existing.path), String.join(".", option.path)));
            paths.sort(Comparator.comparingInt(String::length).thenComparing(Comparator.naturalOrder()));
            ambiguousTypeNames.putIfAbsent(option.typeName, String.format(
                    "Rune choice type %s has two options written with @type '%s', %s and %s, which cannot be told apart",
                    choiceType.getName(), option.typeName, paths.get(0), paths.get(1)));
        }
    }

    static RosettaModelObjectBuilder newBuilder(Class<?> type) throws IOException {
        RuneDataType runeDataType = type.getAnnotation(RuneDataType.class);
        if (runeDataType == null) {
            throw new IOException("Unable to find Rune data type metadata for " + type.getName());
        }
        try {
            return runeDataType.builder().getDeclaredConstructor().newInstance();
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
            throw new IOException("Unable to create Rune builder for " + type.getName(), e);
        }
    }

    private static <T> List<T> append(List<T> list, T element) {
        List<T> result = new ArrayList<>(list);
        result.add(element);
        return Collections.unmodifiableList(result);
    }

    /** One option of the choice, reached by calling {@link #setters} on the builders of each enclosing choice. */
    static final class Option {
        final List<Method> setters;
        final List<String> path;
        final Class<?> type;
        final String typeName;
        final boolean metaWrapped;

        private Option(List<Method> setters, List<String> path, Class<?> type, String typeName, boolean metaWrapped) {
            this.setters = setters;
            this.path = path;
            this.type = type;
            this.typeName = typeName;
            this.metaWrapped = metaWrapped;
        }
    }
}
