package com.regnosys.rosetta.common.serialisation.xml;

/*-
 * ==============
 * Rune Common
 * ==============
 * Copyright (C) 2018 - 2024 REGnosys
 * ==============
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ==============
 */

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;
import com.fasterxml.jackson.dataformat.xml.deser.XmlBeanDeserializerModifier;
import com.fasterxml.jackson.dataformat.xml.ser.XmlBeanSerializerModifier;
import com.rosetta.util.serialisation.RosettaXMLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Using a module class to append our annotation introspector with a minimal fuss
 */
public class RosettaXMLModule extends SimpleModule {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(RosettaXMLModule.class);

    private final RosettaXMLConfiguration rosettaXMLConfiguration;

    private final boolean supportNativeEnumValue;

    private final ObjectMapper mapper;

    private final ClassLoader classLoader;

    private static final ZoneId UNKNOWN_ZONE;

    static {
        ZoneId unknown = null;
        try {
            unknown = ZoneId.of("Unknown");
        } catch (Exception e) {
            LOGGER.error("Failed to create ZoneId for 'Unknown'", e);
        }
        UNKNOWN_ZONE = unknown;
    }

    public RosettaXMLModule(ObjectMapper mapper, final RosettaXMLConfiguration rosettaXMLConfiguration, final boolean supportNativeEnumValue, ClassLoader classLoader) {
        super(RosettaXMLModule.class.getSimpleName());
        this.mapper = mapper;
        this.rosettaXMLConfiguration = rosettaXMLConfiguration;
        this.supportNativeEnumValue = supportNativeEnumValue;
        this.classLoader = classLoader;
    }

    @Override
    public void setupModule(SetupContext context) {
        // Note: order is important. Each modifier is inserted to the front of the list of modifiers.
        final SubstitutionMapLoader substitutionMapLoader = new SubstitutionMapLoader(classLoader);
        context.addBeanSerializerModifier(new RosettaBeanSerializerModifier(substitutionMapLoader));
        context.addBeanSerializerModifier(new XmlBeanSerializerModifier());
        context.addBeanDeserializerModifier(new RosettaBeanDeserializerModifier(substitutionMapLoader));
        context.addBeanDeserializerModifier(new XmlBeanDeserializerModifier(FromXmlParser.DEFAULT_UNNAMED_TEXT_PROPERTY));

        context.insertAnnotationIntrospector(new RosettaXMLAnnotationIntrospector(mapper, rosettaXMLConfiguration, supportNativeEnumValue));

        // Workaround, see https://github.com/REGnosys/rosetta-dsl/issues/663
        addDeserializer(LocalTime.class, new StdDeserializer<LocalTime>(LocalTime.class) {
            @Override
            public LocalTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
                String next = p.readValueAs(String.class);
                try {
                    OffsetTime time = OffsetTime.parse(next, DateTimeFormatter.ISO_TIME);
                    return time.toLocalTime().minusSeconds(time.getOffset().getTotalSeconds());
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

        addDeserializer(ZonedDateTime.class, new StdDeserializer<ZonedDateTime>(ZonedDateTime.class) {
            @Override
            public ZonedDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
                String dateTimeStr = p.readValueAs(String.class);
                try {
                    return DateTimeFormatter.ISO_ZONED_DATE_TIME.parse(dateTimeStr, ZonedDateTime::from);
                } catch (Exception e) {
                    return ZonedDateTime.of(DateTimeFormatter.ISO_LOCAL_DATE_TIME.parse(dateTimeStr, LocalDateTime::from), UNKNOWN_ZONE);
                }
            }
        });
        addSerializer(ZonedDateTime.class, new StdSerializer<ZonedDateTime>(ZonedDateTime.class) {
            @Override
            public void serialize(ZonedDateTime value, JsonGenerator gen, SerializerProvider provider) throws IOException {
                if (value.getZone().equals(UNKNOWN_ZONE)) {
                    gen.writeString(value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                } else {
                    gen.writeString(value.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
                }
            }
        });

        super.setupModule(context);
    }
}
