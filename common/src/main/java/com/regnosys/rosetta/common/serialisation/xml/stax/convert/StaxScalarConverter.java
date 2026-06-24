package com.regnosys.rosetta.common.serialisation.xml.stax.convert;

/*-
 * ==============
 * Rune Common
 * ==============
 * Copyright (C) 2018 - 2026 REGnosys
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

import com.regnosys.rosetta.common.serialisation.xml.UnknownZoneProvider;
import com.regnosys.rosetta.common.serialisation.xml.config.RosettaXMLConfiguration;
import com.regnosys.rosetta.common.serialisation.xml.config.TypeXMLConfiguration;
import com.rosetta.model.lib.ModelSymbolId;
import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;
import com.rosetta.model.lib.records.Date;
import com.rosetta.util.DottedPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.time.zone.ZoneRulesProvider;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Converts between Java scalar values and their XML string representations for the StAX binder.
 *
 * <p>Handles: String, BigDecimal, Integer, Boolean, Date, LocalTime, ZonedDateTime, and enums.
 * Date/time logic is ported from
 * {@link com.regnosys.rosetta.common.serialisation.xml.RosettaXMLModule}.
 */
public class StaxScalarConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(StaxScalarConverter.class);

    private static final ZoneId UNKNOWN_ZONE;

    // Formatters for date + optional offset (no time component), matching RosettaXMLModule
    private static final List<DateTimeFormatter> DATE_WITH_OFFSET_FORMATTERS = Arrays.asList(
            DateTimeFormatter.ofPattern("yyyy-MM-ddXXX"),  // +01:00
            DateTimeFormatter.ofPattern("yyyy-MM-ddXX"),   // +0100
            DateTimeFormatter.ofPattern("yyyy-MM-ddX")     // +01 or Z
    );

    static {
        ZoneId unknown = null;
        try {
            if (!ZoneRulesProvider.getAvailableZoneIds().contains(UnknownZoneProvider.UNKNOWN_ZONE_ID)) {
                ZoneRulesProvider.registerProvider(new UnknownZoneProvider());
            }
            unknown = ZoneId.of(UnknownZoneProvider.UNKNOWN_ZONE_ID);
        } catch (Exception e) {
            LOGGER.error("Failed to create ZoneId for 'Unknown'", e);
        }
        UNKNOWN_ZONE = unknown;
    }

    private final RosettaXMLConfiguration config;

    public StaxScalarConverter(RosettaXMLConfiguration config) {
        this.config = config;
    }

    /**
     * Converts a Java scalar value to its XML string representation.
     * Returns {@code null} when {@code value} is null.
     */
    public String toXmlString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).toPlainString();
        }
        if (value instanceof Integer) {
            return value.toString();
        }
        if (value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof LocalTime) {
            return localTimeToString((LocalTime) value);
        }
        if (value instanceof ZonedDateTime) {
            return zonedDateTimeToString((ZonedDateTime) value);
        }
        if (value instanceof Date) {
            return value.toString();
        }
        if (value instanceof Enum) {
            return enumToString((Enum<?>) value);
        }
        throw new IllegalArgumentException("Unsupported scalar type: " + value.getClass().getName());
    }

    /**
     * Parses an XML string to the given Java target type.
     * Returns {@code null} when {@code text} is null.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object fromXmlString(String text, Class<?> targetType) {
        if (text == null) {
            return null;
        }
        if (String.class.equals(targetType)) {
            return text;
        }
        if (BigDecimal.class.equals(targetType)) {
            return new BigDecimal(text);
        }
        if (Integer.class.equals(targetType) || int.class.equals(targetType)) {
            return Integer.parseInt(text);
        }
        if (Boolean.class.equals(targetType) || boolean.class.equals(targetType)) {
            return Boolean.parseBoolean(text);
        }
        if (LocalTime.class.equals(targetType)) {
            return localTimeFromString(text);
        }
        if (ZonedDateTime.class.equals(targetType)) {
            return zonedDateTimeFromString(text);
        }
        if (Date.class.isAssignableFrom(targetType)) {
            return Date.parse(text);
        }
        if (targetType.isEnum()) {
            return enumFromString(text, (Class<? extends Enum>) targetType);
        }
        throw new IllegalArgumentException("Unsupported scalar type: " + targetType.getName());
    }

    // -------------------------------------------------------------------------
    // LocalTime — ported from RosettaXMLModule
    // -------------------------------------------------------------------------

    private String localTimeToString(LocalTime value) {
        return DateTimeFormatter.ISO_TIME.format(OffsetTime.of(value, ZoneOffset.UTC));
    }

    private LocalTime localTimeFromString(String text) {
        try {
            OffsetTime time = OffsetTime.parse(text, DateTimeFormatter.ISO_TIME);
            return time.toLocalTime().minusSeconds(time.getOffset().getTotalSeconds());
        } catch (DateTimeParseException e) {
            return LocalTime.parse(text, DateTimeFormatter.ISO_TIME);
        }
    }

    // -------------------------------------------------------------------------
    // ZonedDateTime — ported from RosettaXMLModule (5-format cascade)
    // -------------------------------------------------------------------------

    private String zonedDateTimeToString(ZonedDateTime value) {
        if (value.getZone().equals(UNKNOWN_ZONE)) {
            return value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        return value.format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
    }

    private ZonedDateTime zonedDateTimeFromString(String text) {
        // 1. Full ZonedDateTime (includes zone ID like [Europe/Paris])
        try {
            return ZonedDateTime.parse(text, DateTimeFormatter.ISO_ZONED_DATE_TIME);
        } catch (DateTimeParseException ignored) {}

        // 2. OffsetDateTime (has offset but no zone ID)
        try {
            return OffsetDateTime.parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toZonedDateTime();
        } catch (DateTimeParseException ignored) {}

        // 3. LocalDateTime (date + time, no offset)
        try {
            LocalDateTime ldt = LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return ldt.atZone(UNKNOWN_ZONE);
        } catch (DateTimeParseException ignored) {}

        // 4. Date + offset (no time)
        for (DateTimeFormatter formatter : DATE_WITH_OFFSET_FORMATTERS) {
            try {
                TemporalAccessor parsed = formatter.parse(text);
                LocalDate date = LocalDate.from(parsed);
                ZoneOffset offset = ZoneOffset.from(parsed);
                return date.atStartOfDay().atOffset(offset).toZonedDateTime();
            } catch (DateTimeParseException ignored) {}
        }

        // 5. LocalDate only (no time, no offset)
        try {
            LocalDate date = LocalDate.parse(text, DateTimeFormatter.ISO_LOCAL_DATE);
            return date.atStartOfDay(UNKNOWN_ZONE);
        } catch (DateTimeParseException ignored) {}

        throw new IllegalArgumentException("Unrecognized date/time format: " + text);
    }

    // -------------------------------------------------------------------------
    // Enum
    // -------------------------------------------------------------------------

    private String enumToString(Enum<?> value) {
        Optional<Map<String, String>> enumValues = getEnumValues(value.getDeclaringClass());
        if (enumValues.isPresent()) {
            try {
                Field f = value.getDeclaringClass().getField(value.name());
                RosettaEnumValue ann = f.getAnnotation(RosettaEnumValue.class);
                if (ann != null) {
                    String override = enumValues.get().get(ann.value());
                    if (override != null) {
                        return override;
                    }
                }
            } catch (NoSuchFieldException e) {
                // fall through to default serialization
            }
        }
        // Use toDisplayString() when available (generated enums all have it)
        try {
            Method m = value.getDeclaringClass().getMethod("toDisplayString");
            return (String) m.invoke(value);
        } catch (Exception e) {
            return value.toString();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Enum<?> enumFromString(String text, Class<? extends Enum> enumType) {
        Optional<Map<String, String>> enumValues = getEnumValues(enumType);
        Enum<?>[] constants = enumType.getEnumConstants();

        // Config-driven lookup: XML string → enum constant via enumValues map
        if (enumValues.isPresent()) {
            Map<String, String> xmlNames = enumValues.get();
            for (Enum<?> constant : constants) {
                try {
                    Field f = constant.getDeclaringClass().getField(constant.name());
                    RosettaEnumValue ann = f.getAnnotation(RosettaEnumValue.class);
                    if (ann != null) {
                        String xmlName = xmlNames.get(ann.value());
                        if (text.equals(xmlName)) {
                            return constant;
                        }
                    }
                } catch (NoSuchFieldException e) {
                    // skip constant
                }
            }
        }

        // fromDisplayName() static method (generated enums)
        try {
            Method fromDisplayName = enumType.getMethod("fromDisplayName", String.class);
            return (Enum<?>) fromDisplayName.invoke(null, text);
        } catch (Exception ignored) {}

        // toString() match
        for (Enum<?> constant : constants) {
            if (text.equals(constant.toString())) {
                return constant;
            }
        }

        // name() fallback
        return Enum.valueOf(enumType, text);
    }

    private Optional<Map<String, String>> getEnumValues(Class<?> enumType) {
        if (config == null) {
            return Optional.empty();
        }
        RosettaEnum ann = enumType.getAnnotation(RosettaEnum.class);
        if (ann == null) {
            return Optional.empty();
        }
        String namespace = enumType.getPackage().getName();
        ModelSymbolId symbolId = new ModelSymbolId(DottedPath.splitOnDots(namespace), ann.value());
        return config.getConfigurationForType(symbolId).flatMap(TypeXMLConfiguration::getEnumValues);
    }
}
