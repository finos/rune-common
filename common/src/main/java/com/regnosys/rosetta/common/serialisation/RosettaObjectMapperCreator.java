package com.regnosys.rosetta.common.serialisation;

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

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.regnosys.rosetta.common.serialisation.xml.RosettaXmlMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.regnosys.rosetta.common.serialisation.mixin.*;
import com.regnosys.rosetta.common.serialisation.mixin.legacy.LegacyGlobalKeyFieldsMixIn;
import com.regnosys.rosetta.common.serialisation.mixin.legacy.LegacyKeyMixIn;
import com.regnosys.rosetta.common.serialisation.mixin.legacy.LegacyReferenceMixIn;
import com.regnosys.rosetta.common.serialisation.xml.RosettaXMLModule;
import com.regnosys.rosetta.common.serialisation.xml.serialization.RosettaSerialiserFactory;
import com.rosetta.model.lib.meta.GlobalKeyFields;
import com.rosetta.model.lib.meta.Key;
import com.rosetta.model.lib.meta.Reference;
import com.rosetta.model.lib.meta.ReferenceWithMeta;
import com.rosetta.util.serialisation.RosettaXMLConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

/**
 * A lazy-loading holder that returns a pre-configured {@link ObjectMapper} that serves as the default when
 * serialising/deserializing Rosetta Model Objects.
 */
public class RosettaObjectMapperCreator implements ObjectMapperCreator {

    private final Module rosettaModule;
    private final ObjectMapper baseMapper;

    /**
     * If the supportNativeEnumValue is set to true, then the Logical Model enumerations will be used to
     * read and write the enums rather than the Java enum names which are upper case by convention.
     */
    public RosettaObjectMapperCreator(Module rosettaModule, ObjectMapper baseMapper) {
        this.rosettaModule = rosettaModule;
        this.baseMapper = baseMapper;
    }

    public static RosettaObjectMapperCreator forJSON() {
        boolean supportRosettaEnumValue = true;
        ObjectMapper base = new ObjectMapper();
        return new RosettaObjectMapperCreator(new RosettaJSONModule(supportRosettaEnumValue), base);
    }

    public static RosettaObjectMapperCreator forXML(RosettaXMLConfiguration config, ClassLoader classLoader) {
        boolean supportRosettaEnumValue = true;

        // See issue https://github.com/FasterXML/jackson-dataformat-xml/issues/678
        RosettaXmlMapper baseXML = new RosettaXmlMapper(config, classLoader);
        baseXML.setSerializerFactory(RosettaSerialiserFactory.INSTANCE);

        ObjectMapper base = new XmlMapper.Builder(baseXML)
                .defaultUseWrapper(false) // TODO: enable default wrapping after this issue is resolved: https://github.com/FasterXML/jackson-databind/issues/4595
                .build();
        return new RosettaObjectMapperCreator(new RosettaXMLModule(base, config, supportRosettaEnumValue, classLoader), base);
    }

    public static RosettaObjectMapperCreator forXML(RosettaXMLConfiguration config) {
        return forXML(config, RosettaObjectMapperCreator.class.getClassLoader());
    }

    public static RosettaObjectMapperCreator forXML(InputStream configInputStream, ClassLoader classLoader) throws IOException {
        final RosettaXMLConfiguration config = RosettaXMLConfiguration.load(configInputStream);
        return forXML(config, classLoader);
    }

    public static RosettaObjectMapperCreator forXML(InputStream configInputStream) throws IOException {
        return forXML(configInputStream, RosettaObjectMapperCreator.class.getClassLoader());
    }

    public static RosettaObjectMapperCreator forXML() {
        return forXML(new RosettaXMLConfiguration(Collections.emptyMap()), RosettaObjectMapperCreator.class.getClassLoader());
    }

    public static RosettaObjectMapperCreator forCSV() {
        RosettaCsvMapper csvMapper = new RosettaCsvMapper();
        return new RosettaObjectMapperCreator(new RosettaJSONModule(true), csvMapper);
    }

    @Override
    public ObjectMapper create() {
        ObjectMapper mapper = baseMapper
                .registerModule(new GuavaModule())
                .registerModule(new JodaModule())
                .registerModule(new ParameterNamesModule())
                .registerModule(new Jdk8Module())
                .registerModule(new JavaTimeModule())
                .registerModule(new RosettaDateModule())
                .registerModule(rosettaModule)
                .setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
                .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
                .configure(SerializationFeature.WRITE_DATES_WITH_CONTEXT_TIME_ZONE, false)
                .configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false)
                .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)
                //The next two lines add in a filter that excludes the value from a serialised ReferenceWith object if the reference is set
                //the tests for these are in the rosetta-translate project where we have actual rosettaObjects to play with
                .setFilterProvider(new SimpleFilterProvider().addFilter("ReferenceFilter", new ReferenceFilter()))
                .addMixIn(ReferenceWithMeta.class, ReferenceWithMetaMixIn.class)
                //These are needed to support POJOs that were created before the RosettaAttribute annotations were created.
                .addMixIn(GlobalKeyFields.class, LegacyGlobalKeyFieldsMixIn.class)
                .addMixIn(Key.class, LegacyKeyMixIn.class)
                .addMixIn(Reference.class, LegacyReferenceMixIn.class)

                .setVisibility(PropertyAccessor.ALL, Visibility.PUBLIC_ONLY);

        return mapper;
    }
}
