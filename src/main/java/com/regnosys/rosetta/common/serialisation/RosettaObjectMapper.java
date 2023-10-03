package com.regnosys.rosetta.common.serialisation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.blackbird.BlackbirdModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.google.common.io.Resources;
import com.regnosys.rosetta.common.serialisation.mixin.*;
import com.rosetta.model.lib.meta.ReferenceWithMeta;
import com.rosetta.util.serialisation.RosettaXMLConfiguration;

import java.io.IOException;
import java.io.InputStream;

/**
 * Returns a pre-configured {@link ObjectMapper} that serves as the default when
 * serialising/deserializing Rosetta Model Objects.
 */
public class RosettaObjectMapper {

    private RosettaObjectMapper() {
    }

    public static ObjectMapper getNewMinimalRosettaObjectMapper() {
        return new AnnotationBasedObjectMapperCreator(false).create()
                //This call is scans the classpath and can be slow. Use the AnnotationBasedObjectMapperCreator directly if you don't need it.
                .findAndRegisterModules();
    }

    /**
     * Creating new RosettaObjectMapper instances is expensive, use the singleton instance if possible.
     */
    public static ObjectMapper getNewRosettaObjectMapper() {
        return getNewMinimalRosettaObjectMapper()
                .registerModule(new BlackbirdModule());
    }

    public static ObjectMapper getRosettaXMLMapper(final RosettaXMLConfiguration config) {
        return new XmlMapper()
                .registerModule(new RosettaXMLModule(config, true));
    }

    public static ObjectMapper getRosettaXMLMapper(InputStream configInputStream) throws IOException {
        ObjectMapper xmlConfigurationMapper = new ObjectMapper()
                .registerModule(new Jdk8Module()) // because RosettaXMLConfiguration contains `Optional` types.
                .setSerializationInclusion(JsonInclude.Include.NON_ABSENT); // because we want to interpret an absent value as `Optional.empty()`.
        RosettaXMLConfiguration config = xmlConfigurationMapper.readValue(configInputStream, RosettaXMLConfiguration.class);
        return getRosettaXMLMapper(config);
    }
}
