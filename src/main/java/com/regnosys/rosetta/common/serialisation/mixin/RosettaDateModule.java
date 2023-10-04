package com.regnosys.rosetta.common.serialisation.mixin;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.regnosys.rosetta.common.serialisation.mixin.DateExtended;
import com.rosetta.model.lib.records.Date;

import java.io.IOException;

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
    }
}
