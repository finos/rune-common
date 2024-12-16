package poc;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.PackageVersion;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.introspect.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapperCreator;
import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;
import com.rosetta.model.lib.records.Date;
import com.rosetta.model.lib.records.DateImpl;

import java.io.IOException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.function.BiFunction;

import metakey.Root;

public class PocMain {

    public static void main(String[] args) throws JsonProcessingException {
//        RosettaStandaloneSetup rosettaStandaloneSetup = new RosettaStandaloneSetup();
//        Injector injector = rosettaStandaloneSetup.createInjectorAndDoEMFRegistration();
//        CodeGeneratorTestHelper  helper = injector.getInstance(CodeGeneratorTestHelper.class);
//        HashMap<String, String> generateCode = helper.generateCode(rosettaContents());
//        helper.writeClasses(generateCode, "poc");

        System.out.println("Old world serialisation:");
        ObjectMapper oldMapper = RosettaObjectMapperCreator.forJSON().create();

        String oldJson = oldFormatMetaKeyJson();

        System.out.println("Before:");
        System.out.println(oldJson);
        System.out.println("\n\n");

        Root oldRoot = oldMapper.readValue(oldJson, Root.class);

        System.out.println("After:");
        System.out.println(oldMapper.writerWithDefaultPrettyPrinter().writeValueAsString(oldRoot));

        System.out.println("\n\n**********************");
        ObjectMapper objectMapper = create();

        String json = metaKeyJson();

        System.out.println("Before:");
        System.out.println(json);
        System.out.println("\n\n");

        Root root = objectMapper.readValue(json, Root.class);


        System.out.println("After:");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root));
    }


    public static ObjectMapper create() {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new GuavaModule())
                .registerModule(new JodaModule())
                .registerModule(new ParameterNamesModule())
                .registerModule(new Jdk8Module())
                .registerModule(new JavaTimeModule())
                .registerModule(new RosettaDateModule())
                .registerModule(new MyRosettaJSONModule(true))
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
//                .setFilterProvider(new SimpleFilterProvider().addFilter("ReferenceFilter", new ReferenceFilter()))
//                .addMixIn(ReferenceWithMeta.class, ReferenceWithMetaMixIn.class)

                .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.PUBLIC_ONLY);

        return mapper;
    }

    static class RosettaDateModule extends SimpleModule {
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

    static class DateExtended extends DateImpl {
        public DateExtended(@JsonProperty("year") int year, @JsonProperty("month") int month, @JsonProperty("day") int day) {
            super(year, month, day);
        }

        public DateExtended(String date) {
            super(LocalDate.parse(date));
        }
    }


    static class MyRosettaJSONModule extends SimpleModule {

        private static final long serialVersionUID = 1L;
        private final boolean supportRosettaEnumValue;

        public MyRosettaJSONModule(boolean supportRosettaEnumValue) {
            super(PackageVersion.VERSION);
            this.supportRosettaEnumValue = supportRosettaEnumValue;
        }

        @Override
        public void setupModule(SetupContext context) {
            super.setupModule(context);
            context.insertAnnotationIntrospector(new MyRosettaJSONAnnotationIntrospector(supportRosettaEnumValue));
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

    static public class EnumAsStringBuilderIntrospector {

        public void findEnumValues(AnnotatedClass enumType, Enum<?>[] enumValues, String[] names) {
            for (AnnotatedField f : enumType.fields()) {
                final String name = f.getName();
                for (int i = 0, end = enumValues.length; i < end; ++i) {
                    if (name.equals(enumValues[i].name())) {
                        names[i] = enumValues[i].toString();
                    }
                }
            }
        }
    }

    static class RosettaEnumBuilderIntrospector {

        private final EnumNameFunc enumNameFunc;
        private final EnumAliasFunc enumAliasFunc;

        public RosettaEnumBuilderIntrospector(boolean supportRosettaEnumValue) {
            if (supportRosettaEnumValue) {
                this.enumNameFunc = (annotation, javaEnumName) -> !annotation.displayName().isEmpty() ? annotation.displayName() : annotation.value();
            } else {
                this.enumNameFunc = (annotation, javaEnumName) -> !annotation.displayName().isEmpty() ? annotation.displayName() : javaEnumName;
            }
            this.enumAliasFunc = (annotation, javaEnumName) -> !annotation.displayName().isEmpty() ?
                    new String[]{javaEnumName, annotation.displayName(), annotation.value()} :
                    new String[]{javaEnumName, annotation.value()};
        }

        public boolean isApplicable(AnnotatedClass enumType) {
            return enumType.getAnnotation(RosettaEnum.class) != null;
        }

        public void findEnumValues(AnnotatedClass enumType, Enum<?>[] enumValues, String[] names) {
            for (AnnotatedField f : enumType.fields()) {
                if (f.hasAnnotation(RosettaEnumValue.class)) {
                    RosettaEnumValue annotation = f.getAnnotation(RosettaEnumValue.class);
                    final String name = f.getName();
                    for (int i = 0, end = enumValues.length; i < end; ++i) {
                        if (name.equals(enumValues[i].name())) {
                            names[i] = enumNameFunc.apply(annotation, name);
                            break;
                        }
                    }
                }
            }
        }

        public void findEnumAliases(AnnotatedClass enumType, Enum<?>[] enumValues, String[][] aliasList) {
            for (AnnotatedField f : enumType.fields()) {
                if (f.hasAnnotation(RosettaEnumValue.class)) {
                    RosettaEnumValue annotation = f.getAnnotation(RosettaEnumValue.class);
                    final String name = f.getName();
                    for (int i = 0, end = enumValues.length; i < end; ++i) {
                        if (name.equals(enumValues[i].name())) {
                            aliasList[i] = enumAliasFunc.apply(annotation, name);
                            break;
                        }
                    }
                }
            }
        }


        interface EnumNameFunc extends BiFunction<RosettaEnumValue, String, String> {

        }

        interface EnumAliasFunc extends BiFunction<RosettaEnumValue, String, String[]> {

        }
    }

    static class BeanUtil {

        public static String getPropertyName(Method method) {
            String methodName = method.getName();
            String rawPropertyName = getSubstringIfPrefixMatches(methodName, "get");
            if (rawPropertyName == null) {
                rawPropertyName = getSubstringIfPrefixMatches(methodName, "set");
            }

            if (rawPropertyName == null) {
                rawPropertyName = getSubstringIfPrefixMatches(methodName, "is");
            }

            if (rawPropertyName == null) {
                rawPropertyName = getSubstringIfPrefixMatches(methodName, "add");
            }

            return toLowerCamelCase(rawPropertyName);
        }

        public static String toLowerCamelCase(String string) {
            if (string == null) {
                return null;
            } else if (string.isEmpty()) {
                return string;
            } else if (string.length() > 1 && Character.isUpperCase(string.charAt(1)) && Character.isUpperCase(string.charAt(0))) {
                return string;
            } else {
                char[] chars = string.toCharArray();
                chars[0] = Character.toLowerCase(chars[0]);
                return new String(chars);
            }
        }

        private static String getSubstringIfPrefixMatches(String wholeString, String prefix) {
            return wholeString.startsWith(prefix) ? wholeString.substring(prefix.length()) : null;
        }
    }

    static String metaKeyJson() {
        return "{\n" +
                "  \"@model\": \"test.metakey\",\n" +
                "  \"@type\": \"test.metakey.Root\",\n" +
                "  \"@version\": \"0.0.0\",\n" +
                "  \"nodeRef\": {\n" +
                "    \"typeA\": {\n" +
                "      \"@key\": \"someKey\",\n" +
                "      \"@key:external\": \"someExternalKey\",\n" +
                "      \"fieldA\": \"foo\"\n" +
                "    },\n" +
                "    \"aReference\": {\n" +
                "      \"@reference\": \"someKey\",\n" +
                "      \"@ref:external\": \"someExternalKey\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
    }

    static String oldFormatMetaKeyJson() {
        return "{\n" +
                "  \"nodeRef\" : {\n" +
                "    \"typeA\" : {\n" +
                "      \"fieldA\" : \"foo\",\n" +
                "      \"meta\" : {\n" +
                "        \"globalKey\" : \"someKey\",\n" +
                "        \"externalKey\" : \"someExternalKey\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"aReference\" : {\n" +
                "      \"globalReference\" : \"someKey\",\n" +
                "      \"externalReference\" : \"someExternalKey\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
    }

    static String rosettaContents() {
        return "namespace metakey\n" +
                "\n" +
                "annotation rootType: <\"Mark a type as a root of the rosetta model\">\n" +
                "\n" +
                "type A:\n" +
                "  [metadata key]\n" +
                "  fieldA string (1..1)\n" +
                "\n" +
                "type NodeRef:\n" +
                "  typeA A (0..1)\n" +
                "  aReference A (0..1)\n" +
                "    [metadata reference]\n" +
                "\n" +
                "type AttributeRef:\n" +
                "  dateField date (0..1)\n" +
                "    [metadata id]\n" +
                "  dateReference date (0..1)\n" +
                "    [metadata reference]\n" +
                "\n" +
                "type Root:\n" +
                "  [rootType]\n" +
                "  nodeRef NodeRef (0..1)\n" +
                "  attributeRef AttributeRef (0..1)\n";
    }
}
