package com.regnosys.rosetta.common.serialisation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.blackbird.BlackbirdModule;
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

    public static ObjectMapper getRosettaJSONMapper() {
        return new RosettaJSONObjectMapperCreator(false).create();
    }

    public static ObjectMapper getOptimizedRosettaJSONMapper() {
        return getRosettaJSONMapper()
                .registerModule(new BlackbirdModule());
    }

    public static ObjectMapper getRosettaXMLMapper(final RosettaXMLConfiguration config) {
        return new RosettaXMLObjectMapperCreator(config, true).create();
    }

    public static ObjectMapper getRosettaXMLMapper(InputStream configInputStream) throws IOException {
        ObjectMapper xmlConfigurationMapper = new ObjectMapper()
                .registerModule(new Jdk8Module()) // because RosettaXMLConfiguration contains `Optional` types.
                .setSerializationInclusion(JsonInclude.Include.NON_ABSENT); // because we want to interpret an absent value as `Optional.empty()`.
        RosettaXMLConfiguration config = xmlConfigurationMapper.readValue(configInputStream, RosettaXMLConfiguration.class);
        return getRosettaXMLMapper(config);
    }
}
