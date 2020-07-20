package com.regnosys.rosetta.common.serialisation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.json.PackageVersion;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.records.Date;
import com.rosetta.model.lib.records.DateImpl;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.time.LocalDate;

/**
 * A lazy-loading holder that returns a pre-configured {@link ObjectMapper} that serves as the default when
 * serialising/deserializing Rosetta Model Objects.
 */
public class RosettaObjectMapper {

	private RosettaObjectMapper() {
	}

	/**
	 * Creating new RosettaObjectMapper instances is expensive, use the singleton instance if possible.
	 */
	public static ObjectMapper getNewRosettaObjectMapper() {
		return new ObjectMapper().findAndRegisterModules()
								 .registerModule(new KotlinModule())
								 .registerModule(new GuavaModule())
								 .registerModule(new JodaModule())
								 .registerModule(new AfterburnerModule())
								 .registerModule(new ParameterNamesModule())
								 .registerModule(new Jdk8Module())
								 .registerModule(new JavaTimeModule())
								 .registerModule(new RosettaModule())
								 .registerModule(new RosettaDateModule())
								 .setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
								 .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
								 .configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true)
								 .configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true)
								 .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
								 .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true);
	}

	@Deprecated
	public static ObjectMapper getDefaultRosettaObjectMapper() {
		return getNewRosettaObjectMapper();
	}

	/**
	 * Using a module class to append our annotation introspector with a minimal fuss
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

		/**
		 * We generate multiple setters for the builders. When er detect this, we can just choose the correct one here.
		 */
		@Override public AnnotatedMethod resolveSetterConflict(MapperConfig<?> config, AnnotatedMethod setter1, AnnotatedMethod setter2) {
			if (setter1.getParameterCount() == 1 && setter2.getParameterCount() == 1) {
				if (paramIsBuilder(setter1))
					return setter1;
				if (paramIsBuilder(setter2))
					return setter2;
			}
			return super.resolveSetterConflict(config, setter1, setter2);
		}

		private boolean paramIsBuilder(AnnotatedMethod setter) {
			return setter.getParameter(0).getType().isTypeOrSubTypeOf(RosettaModelObject.class);
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

	//TODO remove after Date becomes a class with a default constructor
	private static class RosettaDateModule extends SimpleModule {
		private static final long serialVersionUID = 1L;

		{
			addDeserializer(Date.class, new StdDeserializer<Date>(Date.class) {
				private static final long serialVersionUID = 1L;

				@Override
				public Date deserialize(JsonParser p, DeserializationContext ctxt)
						throws IOException, JsonProcessingException {
					return new DateImpl(p.readValueAs(DateExtended.class).toLocalDate());
				}
			});
		}
	}

	//TODO remove after Date becomes a class with a default constructor
	public static class DateExtended extends DateImpl {
		public DateExtended(@JsonProperty("day") int day, @JsonProperty("month") int month, @JsonProperty("year") int year) {
			super(day, month, year);
		}

		public DateExtended(String date) {
			super(LocalDate.parse(date));
		}
	}
}
