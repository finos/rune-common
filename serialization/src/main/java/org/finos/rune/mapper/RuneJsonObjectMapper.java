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
import com.fasterxml.jackson.core.FormatSchema;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.google.common.annotations.Beta;
import com.rosetta.model.lib.RosettaModelObject;
import org.finos.rune.mapper.date.RuneDateModule;
import org.finos.rune.mapper.filters.SubtypeFilter;
import org.finos.rune.mapper.introspector.RuneJsonModule;
import org.finos.rune.mapper.mixins.RosettaModelObjectMixin;

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
@Beta
public class RuneJsonObjectMapper extends ObjectMapper {
    public RuneJsonObjectMapper() {
        super(create());
    }

    @Override
    protected ObjectWriter _newWriter(SerializationConfig config) {
        return new RuneJsonObjectWriter(this, config);
    }

    @Override
    protected ObjectWriter _newWriter(SerializationConfig config, FormatSchema schema) {
        return new RuneJsonObjectWriter(this, config, schema);
    }

    @Override
    protected ObjectWriter _newWriter(SerializationConfig config, JavaType rootType, PrettyPrinter pp) {
        return new RuneJsonObjectWriter(this, config, rootType, pp);
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
                .setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
                .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
                .configure(SerializationFeature.WRITE_DATES_WITH_CONTEXT_TIME_ZONE, false)
                .configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false)
                .configure(SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS, false)
                .setFilterProvider(new SimpleFilterProvider().addFilter("SubtypeFilter", new SubtypeFilter()))
                .addMixIn(RosettaModelObject.class, RosettaModelObjectMixin.class)
                .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)
                .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.PUBLIC_ONLY);
    }
}
