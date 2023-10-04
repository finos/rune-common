package com.regnosys.rosetta.common.serialisation.mixin;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.rosetta.model.lib.records.Date;

import java.io.IOException;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class RosettaDateModule extends SimpleModule {
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
        addDeserializer(LocalTime.class, new StdDeserializer<LocalTime>(LocalTime.class) {
            @Override
            public LocalTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
                return p.readValueAs(OffsetTime.class).toLocalTime();
            }
        });
        addSerializer(LocalTime.class, new StdSerializer<LocalTime>(LocalTime.class) {
            @Override
            public void serialize(LocalTime value, JsonGenerator gen, SerializerProvider provider) throws IOException {
                gen.writeString(DateTimeFormatter.ISO_OFFSET_TIME.format(OffsetTime.of(value, ZoneOffset.UTC)));
            }
        });
    }
}
