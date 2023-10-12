package com.regnosys.rosetta.common.serialisation;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.module.blackbird.BlackbirdModule;

/**
 * Returns a pre-configured {@link ObjectMapper} that serves as the default when
 * serialising/deserializing Rosetta Model Objects.
 */
public class RosettaObjectMapper {

    private RosettaObjectMapper() {
    }

    public static ObjectMapper getNewMinimalRosettaObjectMapper() {
        return RosettaObjectMapperCreator.forJSON().create()
                // This will scan the classpath and might be slow.
                .findAndRegisterModules();
    }

    /**
     * Creating new RosettaObjectMapper instances is expensive, use the singleton instance if possible.
     */
    public static ObjectMapper getNewRosettaObjectMapper() {
        return getNewMinimalRosettaObjectMapper()
                .registerModule(new BlackbirdModule());
    }
}
