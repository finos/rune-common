package com.regnosys.rosetta.common.serialisation;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.regnosys.rosetta.common.serialisation.mixin.ReferenceFilter;
import com.regnosys.rosetta.common.serialisation.mixin.ReferenceWithMetaMixIn;
import com.regnosys.rosetta.common.serialisation.mixin.RosettaDateModule;
import com.regnosys.rosetta.common.serialisation.mixin.legacy.LegacyGlobalKeyFieldsMixIn;
import com.regnosys.rosetta.common.serialisation.mixin.legacy.LegacyKeyMixIn;
import com.regnosys.rosetta.common.serialisation.mixin.legacy.LegacyReferenceMixIn;
import com.rosetta.model.lib.meta.GlobalKeyFields;
import com.rosetta.model.lib.meta.Key;
import com.rosetta.model.lib.meta.Reference;
import com.rosetta.model.lib.meta.ReferenceWithMeta;

/**
 * A lazy-loading holder that returns a pre-configured {@link ObjectMapper} that serves as the default when
 * serialising/deserializing Rosetta Model Objects.
 */
public class AbstractRosettaObjectMapperCreator implements ObjectMapperCreator {

    private final boolean supportNativeEnumValue;
    private final Module rosettaModule;

    /**
     * If the supportNativeEnumValue is set to true, then the Logical Model enumerations will be used to
     * read and write the enums rather than the Java enum names which are upper case by convention.
     */
    public AbstractRosettaObjectMapperCreator(boolean supportNativeEnumValue, Module rosettaModule) {
        this.supportNativeEnumValue = supportNativeEnumValue;
        this.rosettaModule = rosettaModule;
    }

    protected ObjectMapper createBaseObjectMapper() {
        return new ObjectMapper();
    }

    @Override
    public ObjectMapper create() {
        ObjectMapper mapper = createBaseObjectMapper()
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

        if (supportNativeEnumValue) {
            mapper.configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true);
            mapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
        }
        return mapper;
    }
}
