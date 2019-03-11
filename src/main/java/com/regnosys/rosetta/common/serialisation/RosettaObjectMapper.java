package com.regnosys.rosetta.common.serialisation;

import java.lang.reflect.Modifier;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.json.PackageVersion;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.rosetta.model.lib.RosettaModelObject;

/**
 * A lazy-loading holder that returns a pre-configured {@link ObjectMapper} that serves as the default when
 * serialising/deserializing Rosetta Model Objects.
 */
public class RosettaObjectMapper {

	private RosettaObjectMapper() {
	}

	private static class LazyHolder {
		static final ObjectMapper INSTANCE = new ObjectMapper()	.findAndRegisterModules()
																.registerModule(new GuavaModule())
																.registerModule(new JodaModule())
																.registerModule(new AfterburnerModule())
																.registerModule(new ParameterNamesModule())
																.registerModule(new Jdk8Module())
																.registerModule(new JavaTimeModule())
																.registerModule(new RosettaModule())
																.setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
																.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
																.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true)
																.configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true)
																.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
																.configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true);
	}

	public static ObjectMapper getDefaultRosettaObjectMapper() {
		return LazyHolder.INSTANCE;
	}

	/**
	 * Using a module class to append our annotation introspector with a minimal fuss 
	 * 
	 */
	private static class RosettaModule extends SimpleModule {
		
		private static final long serialVersionUID = 1L;

		public RosettaModule() {
			super(PackageVersion.VERSION);
		}

		@Override
		public void setupModule(SetupContext context) {
			super.setupModule(context);
			context.insertAnnotationIntrospector(new RosettaBuilderIntrospector());
		}

		@Override
		public int hashCode() {
			return getClass().hashCode();
		}

		@Override
		public boolean equals(Object o) {
			return this == o;
		}
	}

	private static class RosettaBuilderIntrospector extends JacksonAnnotationIntrospector {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public Class<?> findPOJOBuilder(AnnotatedClass ac) {
			JavaType type = ac.getType();
			// [Ljava.lang.String  type is null!
			if (null != type) {
				Class<?> rawClass = type.getRawClass();

				if (!Modifier.isAbstract(rawClass.getModifiers()) && RosettaModelObject.class.isAssignableFrom(rawClass)) {
					try {
						return Class.forName(rawClass.getTypeName() + "$" + rawClass.getSimpleName() + "Builder",
								true, rawClass.getClassLoader());

					} catch (ClassNotFoundException e) {
						throw new RosettaSerialiserException("Could not find the builder class for " + rawClass, e);
					}
				}
			}
			return super.findPOJOBuilder(ac);
		}

		@Override
		public Version version() {
			return Version.unknownVersion();
		}
	}

	private static class RosettaSerialiserException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		RosettaSerialiserException(String message, Throwable cause) {
			super(message, cause);
		}
	}

}
