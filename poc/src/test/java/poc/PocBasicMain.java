package poc;

import annotations.RosettaModelObjectMixin;
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
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.google.inject.Injector;
import com.regnosys.rosetta.RosettaStandaloneSetup;
import com.regnosys.rosetta.tests.util.CodeGeneratorTestHelper;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.records.Date;
import com.rosetta.model.lib.records.DateImpl;
import test.basic.Root;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;

public class PocBasicMain {

    public static void main(String[] args) throws JsonProcessingException {
//        RosettaStandaloneSetup rosettaStandaloneSetup = new RosettaStandaloneSetup();
//        Injector injector = rosettaStandaloneSetup.createInjectorAndDoEMFRegistration();
//        CodeGeneratorTestHelper helper = injector.getInstance(CodeGeneratorTestHelper.class);
//        HashMap<String, String> generateCode = helper.generateCode(rosettaContents());
//        helper.writeClasses(generateCode, "poc");

        System.out.println("\n\n********************** Basic Single");
        ObjectMapper basicSingleMapper = create();

        String basicSingleJson = basicSingleJson();

        System.out.println("Before:");
        System.out.println(basicSingleJson);
        System.out.println("\n\n");

        Root basicSingleRoot = basicSingleMapper.readValue(basicSingleJson, Root.class);

        System.out.println("After:");
        System.out.println(basicSingleMapper.writerWithDefaultPrettyPrinter().writeValueAsString(basicSingleRoot));

        System.out.println("\n\n********************** Basic List");
        ObjectMapper basicListMapper = create();

        String basicListJson = basicListJson();

        System.out.println("Before:");
        System.out.println(basicListJson);
        System.out.println("\n\n");

        Root basicListRoot = basicListMapper.readValue(basicListJson, Root.class);

        System.out.println("After:");
        System.out.println(basicListMapper.writerWithDefaultPrettyPrinter().writeValueAsString(basicListRoot));

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
                .setFilterProvider(new SimpleFilterProvider().addFilter("SubTypeFilter", new SubTypeFilter()))
                .addMixIn(RosettaModelObject.class, RosettaModelObjectMixin.class)
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

    static String basicSingleJson() {
        return "{\n" +
                "  \"@model\": \"test.basic\",\n" +
                "  \"@type\": \"test.basic.Root\",\n" +
                "  \"@version\": \"0.0.0\",\n" +
                "  \"basicSingle\": {\n" +
                "    \"booleanType\": true,\n" +
                "    \"numberType\": 123.456,\n" +
                "    \"parameterisedNumberType\": 123.99,\n" +
                "    \"parameterisedStringType\": \"abcDEF\",\n" +
                "    \"stringType\": \"foo\",\n" +
                "    \"timeType\": \"12:00:00\"\n" +
                "  }\n" +
                "}";
    }

    static String basicListJson() {
        return "{\n" +
                "  \"@model\": \"test.basic\",\n" +
                "  \"@type\": \"test.basic.Root\",\n" +
                "  \"@version\": \"0.0.0\",\n" +
                "  \"basicList\": {\n" +
                "    \"booleanTypes\": [true, false, true],\n" +
                "    \"numberTypes\": [123.456, 789, 345.123],\n" +
                "    \"parameterisedNumberTypes\": [123.99, 456, 99.12],\n" +
                "    \"parameterisedStringTypes\": [\"abcDEF\", \"foo\", \"foo\"],\n" +
                "    \"stringTypes\": [\"foo\", \"bar\", \"Baz123\"],\n" +
                "    \"timeTypes\": [\"12:00:00\"]\n" +
                "  }\n" +
                "}";
    }

    static String rosettaContents() {
        return "namespace basic\n" +
                "\n" +
                "annotation rootType: <\"Mark a type as a root of the rosetta model\">\n" +
                "\n" +
                "typeAlias ParameterisedNumberType:\n" +
                "\tnumber(digits: 18, fractionalDigits: 2)\n" +
                "\n" +
                "typeAlias ParameterisedStringType:\n" +
                "    string(minLength: 1, maxLength: 20, pattern: \"[a-zA-Z]\")\n" +
                "\n" +
                "type BasicSingle:\n" +
                "  booleanType boolean (1..1)\n" +
                "  numberType number (1..1)\n" +
                "  parameterisedNumberType ParameterisedNumberType (1..1)\n" +
                "  parameterisedStringType ParameterisedStringType (1..1)\n" +
                "  stringType string (1..1)\n" +
                "  timeType time (1..1)\n" +
                "\n" +
                "type BasicList:\n" +
                "  booleanTypes boolean (1..*)\n" +
                "  numberTypes number (1..*)\n" +
                "  parameterisedNumberTypes ParameterisedNumberType (1..*)\n" +
                "  stringTypes string (1..*)\n" +
                "  timeTypes time (1..*)\n" +
                "\n" +
                "type Root:\n" +
                "  [rootType]\n" +
                "  basicSingle BasicSingle (0..1)\n" +
                "  basicList BasicList (0..1)\n" +
                "    ";
    }
}
