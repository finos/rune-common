package com.regnosys.rosetta.common.serialisation;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.json.PackageVersion;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.blackbird.BlackbirdModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaClass;
import com.rosetta.model.lib.meta.ReferenceWithMeta;
import com.rosetta.model.lib.records.Date;
import com.rosetta.model.lib.records.DateImpl;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * A lazy-loading holder that returns a pre-configured {@link ObjectMapper} that serves as the default when
 * serialising/deserializing Rosetta Model Objects.
 */
public class RosettaObjectMapper {

	private RosettaObjectMapper() {
	}

	public static ObjectMapper getNewMinimalRosettaObjectMapper() {
		return new ObjectMapper().findAndRegisterModules()
				.registerModule(new GuavaModule())
				.registerModule(new JodaModule())
				.registerModule(new ParameterNamesModule())
				.registerModule(new Jdk8Module())
				.registerModule(new JavaTimeModule())
				.registerModule(new RosettaModule())
				.registerModule(new RosettaDateModule())
				.setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
				.setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
				.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
				.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true)
				.configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true)
				.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
				.configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
				.enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)
				//The next two lines add in a filter that excludes the value from a serialised ReferenceWith object if the reference is set
				//the tests for these are in the rosetta-translate project where we have actual rosettaObjects to play with
				.setFilterProvider(new SimpleFilterProvider().addFilter("ReferenceFilter", new ReferenceFilter()))
				.addMixIn(ReferenceWithMeta.class, RosettaObjectMapper.ReferenceWithMetaMixIn.class)
				.setVisibility(PropertyAccessor.ALL, Visibility.PUBLIC_ONLY);
	}

	/**
	 * Creating new RosettaObjectMapper instances is expensive, use the singleton instance if possible.
	 */
	public static ObjectMapper getNewRosettaObjectMapper() {
		return getNewMinimalRosettaObjectMapper()
								.registerModule(new BlackbirdModule());

	}

	@Deprecated
	public static ObjectMapper getDefaultRosettaObjectMapper() {
		return getNewRosettaObjectMapper();
	}

	/**
	 * Using a module class to append our annotation introspector with a minimal fuss
	 */
	protected static class RosettaModule extends SimpleModule {

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

	protected static class RosettaBuilderIntrospector extends JacksonAnnotationIntrospector implements BackwardsCompatibleAnnotationIntrospector {

		private static final long serialVersionUID = 1L;

		@Override
		public Class<?> findPOJOBuilder(AnnotatedClass ac) {
			if (ac.hasAnnotation(RosettaClass.class)) {
				return ac.getAnnotation(RosettaClass.class).builder();
			}
			return super.findPOJOBuilder(ac);
		}

		@Override
		public PropertyName findNameForSerialization(Annotated a) {
			if (a.hasAnnotation(RosettaAttribute.class)) {
				return new PropertyName(a.getAnnotation(RosettaAttribute.class).value());
			}
			return super.findNameForSerialization(a);
		}

		@Override
		public PropertyName findNameForDeserialization(Annotated a) {
			if (a.hasAnnotation(RosettaAttribute.class)) {
				return new PropertyName(a.getAnnotation(RosettaAttribute.class).value());
			}
			return super.findNameForDeserialization(a);
		}

		@Override
		public JsonIgnoreProperties.Value findPropertyIgnoralByName(MapperConfig<?> config, Annotated ann) {
			return findPropertyIgnorals(ann);
		}

		@Deprecated
		@Override
		public JsonIgnoreProperties.Value findPropertyIgnorals(Annotated ac) {
			if (ac instanceof AnnotatedClass && ac.hasAnnotation(RosettaClass.class)) {
				AnnotatedClass acc = (AnnotatedClass) ac;
				Set<String> includes = getPropertyNames(acc, x -> x.hasAnnotation(RosettaAttribute.class));
				Set<String> ignored = getPropertyNames(acc, x -> !x.hasAnnotation(RosettaAttribute.class));
				ignored.removeAll(includes);
				return JsonIgnoreProperties.Value.forIgnoredProperties(ignored).withAllowSetters();
			}
			return JsonIgnoreProperties.Value.empty();
		}

		private static Set<String> getPropertyNames(AnnotatedClass acc, Predicate<AnnotatedMethod> filter) {
			return StreamSupport.stream(acc.memberMethods().spliterator(), false)
					.filter(filter)
					.map(m -> BeanUtil.getPropertyName(m.getAnnotated()))
					.filter(Objects::nonNull)
					.collect(Collectors.toSet());
		}

		@Override
		public Version version() {
			return Version.unknownVersion();
		}
	}

	protected static class RosettaDateModule extends SimpleModule {
		private static final long serialVersionUID = 1L;

		{
			addDeserializer(Date.class, new StdDeserializer<Date>(Date.class) {
				@Override
				public Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
					return Date.of(p.readValueAs(DateExtended.class).toLocalDate());
				}
			});
			addSerializer(Date.class, new StdSerializer<Date>(Date.class) {
				@Override
				public void serialize(Date value, JsonGenerator gen, SerializerProvider provider) throws IOException {
					gen.writeString(value.toString());
				}
			});
		}
	}

	public static class DateExtended extends DateImpl {
		public DateExtended(@JsonProperty("year") int year, @JsonProperty("month") int month, @JsonProperty("day") int day) {
			super(year, month, day);
		}

		public DateExtended(String date) {
			super(LocalDate.parse(date));
		}
	}

	//This class serves to ensure that the value of a reference doesn't get serialized if the
	//reference or global key field is populated
	public static class ReferenceFilter extends SimpleBeanPropertyFilter {

		@Override
		public void serializeAsField(Object pojo, JsonGenerator jgen, SerializerProvider provider,
									 PropertyWriter writer) throws Exception {
			if (!filterOut(pojo, writer.getName())) {
				writer.serializeAsField(pojo, jgen, provider);
			}
		}

		@Override
		public void serializeAsField(Object bean, JsonGenerator jgen, SerializerProvider provider,
									 BeanPropertyWriter writer) throws Exception {
			if (!filterOut(bean, writer.getName())) {
				writer.serializeAsField(bean, jgen, provider);
			}
		}

		private boolean filterOut(Object pojo, String name) {
			if (!name.equals("value")) return false;
			if (pojo instanceof ReferenceWithMeta) {
				return hasReference((ReferenceWithMeta<?>) pojo);
			}
			return false;
		}

		private boolean hasReference(ReferenceWithMeta<?> pojo) {
			return pojo.getGlobalReference() != null || (pojo.getReference() != null && pojo.getReference().getReference() != null);
		}
	}

	@JsonFilter("ReferenceFilter")
	protected interface ReferenceWithMetaMixIn {
	}
}
