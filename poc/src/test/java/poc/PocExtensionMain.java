package poc;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.PackageVersion;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.rosetta.model.lib.records.Date;
import com.rosetta.model.lib.records.DateImpl;
import test.extension.Root;

import java.io.IOException;
import java.time.LocalDate;

public class PocExtensionMain {

    public static void main(String[] args) throws JsonProcessingException {
//        RosettaStandaloneSetup rosettaStandaloneSetup = new RosettaStandaloneSetup();
//        Injector injector = rosettaStandaloneSetup.createInjectorAndDoEMFRegistration();
//        CodeGeneratorTestHelper helper = injector.getInstance(CodeGeneratorTestHelper.class);
//        HashMap<String, String> generateCode = helper.generateCode(rosettaContents());
//        helper.writeClasses(generateCode, "poc");
//
        System.out.println("\n\n********************** Extension Base");
        ObjectMapper extensionBaseMapper = create();

        String extensionBaseJson = extensionBaseJson();

        System.out.println("Before:");
        System.out.println(extensionBaseJson);
        System.out.println("\n\n");

        Root extensionBaseRoot = extensionBaseMapper.readValue(extensionBaseJson, Root.class);

        System.out.println("After:");
        System.out.println(extensionBaseMapper.writerWithDefaultPrettyPrinter().writeValueAsString(extensionBaseRoot));

        System.out.println("\n\n********************** Extension Concreate");
        ObjectMapper extensionConcreteMapper = create();

        String extensionConcreteJson = extensionConcreteJson();

        System.out.println("Before:");
        System.out.println(extensionConcreteJson);
        System.out.println("\n\n");

        Root extensionConcreteRoot = extensionConcreteMapper.readValue(extensionConcreteJson, Root.class);

        System.out.println("After:");
        System.out.println(extensionConcreteMapper.writerWithDefaultPrettyPrinter().writeValueAsString(extensionConcreteRoot));

        System.out.println("\n\n********************** Extension Polymorphic");
        ObjectMapper extensionPolymorphicMapper = create();

        String extensionPolymorphicJson = extensionPolymorphicJson();

        System.out.println("Before:");
        System.out.println(extensionPolymorphicJson);
        System.out.println("\n\n");

        Root extensionPolymorphicRoot = extensionPolymorphicMapper.readValue(extensionPolymorphicJson, Root.class);

        System.out.println("After:");
        System.out.println(extensionPolymorphicMapper.writerWithDefaultPrettyPrinter().writeValueAsString(extensionPolymorphicRoot));
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
                .configure(MapperFeature.REQUIRE_TYPE_ID_FOR_SUBTYPES, false)
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

    static String extensionBaseJson() {
        return "{\n" +
                "  \"@model\": \"test\",\n" +
                "  \"@type\": \"test.extension.Root\",\n" +
                "  \"@version\": \"0.0.0\",\n" +
                "  \"typeA\": {\n" +
                "    \"fieldA\": \"foo\"\n" +
                "  }\n" +
                "}";
    }

    static String extensionConcreteJson() {
        return "{\n" +
                "  \"@model\": \"test\",\n" +
                "  \"@type\": \"test.extension.Root\",\n" +
                "  \"@version\": \"0.0.0\",\n" +
                "  \"typeB\": {\n" +
                "    \"fieldB\": \"foo\"\n" +
                "  }\n" +
                "}";
    }

    static String extensionPolymorphicJson() {
        return "{\n" +
                "  \"@model\": \"test\",\n" +
                "  \"@type\": \"test.extension.Root\",\n" +
                "  \"@version\": \"0.0.0\",\n" +
                "  \"typeA\": {\n" +
                "    \"@type\": \"test.extension.B\",\n" +
                "    \"fieldB\": \"foo\",\n" +
                "    \"fieldA\": \"bar\"\n" +
                "  }\n" +
                "}";
    }

    static String rosettaContents() {
        return "namespace extension\n" +
                "\n" +
                "annotation rootType: <\"Mark a type as a root of the rosetta model\">\n" +
                "\n" +
                "\n" +
                "type A:\n" +
                "  fieldA string (1..1)\n" +
                "\n" +
                "type B extends A:\n" +
                "  fieldB string (1..1)\n" +
                "\n" +
                "type Root:\n" +
                "  [rootType]\n" +
                "  typeA A (0..1)\n" +
                "  typeB B (0..1)\n" +
                "\n";
    }
}
