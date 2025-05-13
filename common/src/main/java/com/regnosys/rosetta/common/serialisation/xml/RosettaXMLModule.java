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
import com.google.common.collect.Lists;
import com.rosetta.util.serialisation.RosettaXMLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.List;

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

    // Formatters for date + optional offset
    private static final List<DateTimeFormatter> DATE_WITH_OFFSET_FORMATTERS = Lists.newArrayList(
            DateTimeFormatter.ofPattern("yyyy-MM-ddXXX"),  // +01:00
            DateTimeFormatter.ofPattern("yyyy-MM-ddXX"),   // +0100
            DateTimeFormatter.ofPattern("yyyy-MM-ddX")     // +01 or Z
    );

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
                // 1. Full ZonedDateTime (includes zone ID like [Europe/Paris])
                try {
                    return ZonedDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_ZONED_DATE_TIME);
                } catch (DateTimeParseException ignored) {}

                // 2. OffsetDateTime (has offset but no zone ID)
                try {
                    return OffsetDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toZonedDateTime();
                } catch (DateTimeParseException ignored) {}

                // 3. LocalDateTime (date + time, no offset)
                try {
                    LocalDateTime ldt = LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    return ldt.atZone(UNKNOWN_ZONE);
                } catch (DateTimeParseException ignored) {}

                // 4. Date + offset (no time)
                for (DateTimeFormatter formatter : DATE_WITH_OFFSET_FORMATTERS) {
                    try {
                        TemporalAccessor parsed = formatter.parse(dateTimeStr);
                        LocalDate date = LocalDate.from(parsed);
                        ZoneOffset offset = ZoneOffset.from(parsed);
                        return date.atStartOfDay().atOffset(offset).toZonedDateTime();
                    } catch (DateTimeParseException ignored) {}
                }

                // 5. LocalDate only (no time, no offset)
                try {
                    LocalDate date = LocalDate.parse(dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE);
                    return date.atStartOfDay(UNKNOWN_ZONE);
                } catch (DateTimeParseException ignored) {}

                // No match
                throw new IllegalArgumentException("Unrecognized date/time format: " + dateTimeStr);
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
