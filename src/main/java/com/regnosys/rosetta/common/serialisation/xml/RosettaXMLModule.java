package com.regnosys.rosetta.common.serialisation.xml;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.rosetta.util.serialisation.RosettaXMLConfiguration;

import java.io.IOException;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Using a module class to append our annotation introspector with a minimal fuss
 */
public class RosettaXMLModule extends SimpleModule {

    private static final long serialVersionUID = 1L;

    private final RosettaXMLConfiguration rosettaXMLConfiguration;

    private final boolean supportNativeEnumValue;


    public RosettaXMLModule(final RosettaXMLConfiguration rosettaXMLConfiguration, final boolean supportNativeEnumValue) {
        super(RosettaXMLModule.class.getSimpleName());
        this.rosettaXMLConfiguration = rosettaXMLConfiguration;
        this.supportNativeEnumValue = supportNativeEnumValue;
    }

    @Override
    public void setupModule(SetupContext context) {
        context.insertAnnotationIntrospector(new RosettaXMLAnnotationIntrospector(rosettaXMLConfiguration,supportNativeEnumValue));

        // Workaround, see https://github.com/REGnosys/rosetta-dsl/issues/663
        addDeserializer(LocalTime.class, new StdDeserializer<LocalTime>(LocalTime.class) {
            @Override
            public LocalTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
                String next = p.readValueAs(String.class);
                try {
                    OffsetTime time = OffsetTime.parse(next, DateTimeFormatter.ISO_TIME);
                    return time.toLocalTime().plusSeconds(time.getOffset().getTotalSeconds());
                } catch (DateTimeParseException e) {
                    return LocalTime.parse(next, DateTimeFormatter.ISO_TIME);
                }
            }
        });
        addSerializer(LocalTime.class, new StdSerializer<LocalTime>(LocalTime.class) {
            @Override
            public void serialize(LocalTime value, JsonGenerator gen, SerializerProvider provider) throws IOException {
                gen.writeString(DateTimeFormatter.ISO_TIME.format(OffsetTime.of(value, ZoneOffset.UTC)));
            }
        });

        super.setupModule(context);
    }
}
