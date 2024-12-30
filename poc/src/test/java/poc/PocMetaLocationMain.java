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
import test.metalocation.Root;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.HashMap;

public class PocMetaLocationMain {

    public static void main(String[] args) throws JsonProcessingException {
//        RosettaStandaloneSetup rosettaStandaloneSetup = new RosettaStandaloneSetup();
//        Injector injector = rosettaStandaloneSetup.createInjectorAndDoEMFRegistration();
//        CodeGeneratorTestHelper helper = injector.getInstance(CodeGeneratorTestHelper.class);
//        HashMap<String, String> generateCode = helper.generateCode(rosettaContents());
//        helper.writeClasses(generateCode, "poc");


        runTest("Meta location address", metaLocationAddress());

        runTest("Meta location dangling address", metaLocationDanglingAddress());

    }

    private static void runTest(String name, String inputJson) throws JsonProcessingException {
        System.out.println("\n\n********************** " + name);
        ObjectMapper mapper = create();

        System.out.println("Before:");
        System.out.println(inputJson);
        System.out.println("\n\n");

        Root root = mapper.readValue(inputJson, Root.class);

        System.out.println("After:");
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root));
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
                .configure(SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS, false)
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

    static String metaLocationAddress() {
        return readAsString(Paths.get("./serialization/src/test/resources/rune-serializer-round-trip-test/meta-location/address.json"));

    }

    static String metaLocationDanglingAddress() {
        return readAsString(Paths.get("./serialization/src/test/resources/rune-serializer-round-trip-test/meta-location/dangling-address.json"));
    }

    public static String readAsString(Path jsonPath) {
        try {
            return new String(Files.readAllBytes(jsonPath));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    static String rosettaContents() {
        return readAsString(Paths.get("./serialization/src/test/resources/rune-serializer-round-trip-test/meta-location/meta-location.rosetta"));
    }
}
