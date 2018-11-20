package com.regnosys.rosetta.common.serialisation;

import java.lang.reflect.Modifier;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rosetta.model.lib.RosettaModelObject;

/**
 * A lazy-loading holder that returns a pre-configured {@link ObjectMapper} that serves as the default when
 * serialising/deserializing Rosetta Model Objects.
 */
public class RosettaObjectMapper {

    private RosettaObjectMap per() {}

    private static class LazyHolder {
        static final ObjectMapper INSTANCE = new ObjectMapper()
                .findAndRegisterModules()
                .registerModule(new JavaTimeModule())
                .registerModule(new Jdk8Module())
                .setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
                .setAnnotationIntrospector(new RosettaBuilderIntrospector());
    }

    public static ObjectMapper getDefaultRosettaObjectMapper() {
        return LazyHolder.INSTANCE;
    }

    private static class RosettaBuilderIntrospector extends JacksonAnnotationIntrospector {

        public Class<?> findPOJOBuilder(AnnotatedClass ac) {
            Class<?> rawClass = ac.getType().getRawClass();

            if (!Modifier.isAbstract(rawClass.getModifiers()) && RosettaModelObject.class.isAssignableFrom(rawClass)) {
                try {
                    return Class.forName(rawClass.getTypeName() + "$" + rawClass.getSimpleName() + "Builder",
                            true, rawClass.getClassLoader());

                } catch (ClassNotFoundException e) {
                    throw new RosettaSerialiserException("Could not find the builder class for " + rawClass, e);
                }
            }

            return super.findPOJOBuilder(ac);
        }

        @Override
        public Version version() {
            return null;
        }
    }

    private static class RosettaSerialiserException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        RosettaSerialiserException(String message, Throwable cause) {
            super(message, cause);
        }
    }


}
