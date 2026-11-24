package org.finos.rune.mapper;

/*-
 * ==============
 * Rune Common
 * ==============
 * Copyright (C) 2018 - 2024 REGnosys
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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.JsonNodeFeature;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.rosetta.model.lib.RosettaModelObject;
import org.finos.rune.mapper.attributesonly.AttributesOnlySerializerProvider;
import org.finos.rune.mapper.attributesonly.AttributesOnlyStripper;
import org.finos.rune.mapper.attributesonly.ModelReadRejectingIntrospector;
import org.finos.rune.mapper.date.RuneDateModule;
import org.finos.rune.mapper.filters.SubtypeFilter;
import org.finos.rune.mapper.introspector.RuneJsonModule;
import org.finos.rune.mapper.mixins.RosettaModelObjectMixin;

import java.util.List;

/**
 * A custom {@link ObjectMapper} designed for the Rune DSL.
 * <p>
 * This class provides JSON serialization and deserialization that closely
 * aligns with the structure of the Rune DSL, ensuring that the serialized
 * output is both human-readable and faithful to the DSL's design.
 * </p>
 *
 * <p>
 * By tailoring the serialization format, this mapper makes the JSON output
 * intuitive for developers and domain experts working with the Rune DSL,
 * while maintaining compatibility with standard JSON processing tools.
 * </p>
 *
 * <h2>Usage:</h2>
 * <pre>
 * RuneJacksonJsonObjectMapper objectMapper = new RuneJacksonJsonObjectMapper();
 * String json = objectMapper.writeValueAsString(runeObject);
 * </pre>
 *
 * IMPORTANT NOTE: This class is not yet production ready and is still under
 * development please use {@code RosettaObjectMapper} in the meantime
 *
 * @see ObjectMapper
 */
public class RuneJsonObjectMapper extends ObjectMapper {
    private final boolean attributesOnly;

    public RuneJsonObjectMapper() {
        this(false);
    }

    public RuneJsonObjectMapper(ClassLoader classLoader) {
        this(false);
        useClassLoader(classLoader);
    }

    private RuneJsonObjectMapper(boolean attributesOnly) {
        super(create());
        this.attributesOnly = attributesOnly;
        if (attributesOnly) {
            setSerializerProvider(new AttributesOnlySerializerProvider(this));
            DeserializationConfig deserializationConfig = getDeserializationConfig();
            setConfig(deserializationConfig.with(AnnotationIntrospector.pair(
                    new ModelReadRejectingIntrospector(), deserializationConfig.getAnnotationIntrospector())));
        }
    }

    /**
     * A mapper that writes only model attributes: no {@code @}-prefixed property anywhere, each
     * {@code {"@data": ...}} written as its bare value, and an attribute held only as a reference omitted.
     * See {@link AttributesOnlyStripper} for the rules.
     * <p>
     * The JSON it writes cannot be read back, so the mapper refuses to read model objects.
     */
    public static RuneJsonObjectMapper attributesOnly() {
        return new RuneJsonObjectMapper(true);
    }

    /**
     * {@link #attributesOnly()}, resolving model types against {@code classLoader}.
     */
    public static RuneJsonObjectMapper attributesOnly(ClassLoader classLoader) {
        RuneJsonObjectMapper mapper = new RuneJsonObjectMapper(true);
        mapper.useClassLoader(classLoader);
        return mapper;
    }

    public boolean isAttributesOnly() {
        return attributesOnly;
    }

    private void useClassLoader(ClassLoader classLoader) {
        setTypeFactory(getTypeFactory().withClassLoader(classLoader));
    }

    public static List<String> getMetaProperties() {
        return RuneJsonPropertyNames.getMetaProperties();
    }

    private static ObjectMapper create() {
        return new ObjectMapper()
                .registerModule(new GuavaModule())
                .registerModule(new JodaModule())
                .registerModule(new ParameterNamesModule())
                .registerModule(new Jdk8Module())
                .registerModule(new JavaTimeModule())
                .registerModule(new RuneDateModule())
                .registerModule(new RuneJsonModule())
                .setDefaultPropertyInclusion(JsonInclude.Include.NON_EMPTY)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
                .configure(SerializationFeature.WRITE_DATES_WITH_CONTEXT_TIME_ZONE, false)
                .configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false)
                .configure(SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS, false)
                .configure(JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES, false)
                .setFilterProvider(new SimpleFilterProvider().addFilter("SubtypeFilter", new SubtypeFilter()))
                .addMixIn(RosettaModelObject.class, RosettaModelObjectMixin.class)
                .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)
                .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.PUBLIC_ONLY)
                // Always pretty print with "\n": serialised documents are stored and
                // compared across operating systems
                .setDefaultPrettyPrinter(new DefaultPrettyPrinter().withObjectIndenter(new DefaultIndenter("  ", "\n")));
    }
}
