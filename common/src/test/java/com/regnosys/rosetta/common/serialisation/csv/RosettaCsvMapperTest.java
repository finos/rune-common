package com.regnosys.rosetta.common.serialisation.csv;

/*-
 * ==============
 * Rune Common
 * ==============
 * Copyright (C) 2018 - 2025 REGnosys
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

import com.fasterxml.jackson.databind.JsonMappingException;
import com.regnosys.rosetta.common.serialisation.RosettaCsvMapper;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapperCreator;
import com.regnosys.rosetta.common.serialisation.csv.config.CsvDialect;
import com.regnosys.rosetta.common.serialisation.csv.config.RosettaCSVConfiguration;
import csv.test.user.User;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RosettaCsvMapperTest {

    @Test
    void testCsvMapperSerialise() throws IOException {
        RosettaCsvMapper csvObjectMapper = RosettaCsvMapper.createCsvObjectMapper();
        User user = User.builder()
                .setFirstName("FirstName")
                .setLastName("LastName")
                .setIdentifier("identifier")
                .setUsername("username")
                .build();

        String serializedKey = csvObjectMapper.writeValueAsString(user);

        String expected = "username,identifier,firstName,lastName\n" +
                "username,identifier,FirstName,LastName\n";
        assertEquals(expected, serializedKey);
    }

    @Test
    void testCsvMapperDeserialize() throws JsonMappingException {
        RosettaCsvMapper csvObjectMapper = RosettaCsvMapper.createCsvObjectMapper();
        String input = "firstName,identifier,lastName,username\n" +
                "FirstName,identifier,LastName,username\n";

        User user = csvObjectMapper.readValue(input, User.class);

        User expected = User.builder()
                .setFirstName("FirstName")
                .setLastName("LastName")
                .setIdentifier("identifier")
                .setUsername("username")
                .build();

        assertEquals(expected, user);
    }

    @Test
    void testCsvMapperDeserializeIgnoresExtraLines() throws JsonMappingException {
        RosettaCsvMapper csvObjectMapper = RosettaCsvMapper.createCsvObjectMapper();
        String input = "firstName,identifier,lastName,username\n" +
                "FirstName,identifier,LastName,username\n" +
                "FirstName2,identifier2,LastName2,username2\n";

        User user = csvObjectMapper.readValue(input, User.class);

        User expected = User.builder()
                .setFirstName("FirstName")
                .setLastName("LastName")
                .setIdentifier("identifier")
                .setUsername("username")
                .build();

        assertEquals(expected, user);
    }

    @Test
    void testCsvMapperRoundTrip() throws IOException {
        RosettaCsvMapper csvObjectMapper = RosettaCsvMapper.createCsvObjectMapper();
        User user = User.builder()
                .setFirstName("FirstName")
                .setLastName("LastName")
                .setIdentifier("identifier")
                .setUsername("username")
                .build();

        String serializedKey = csvObjectMapper.writeValueAsString(user);
        User newUser = csvObjectMapper.readValue(serializedKey, User.class);

        assertEquals(user.build(), newUser);
    }

    @Test
    void testSemicolonDialectRoundTrip() throws IOException {
        RosettaCSVConfiguration config = RosettaCSVConfiguration.builder()
                .setDialect(CsvDialect.builder().setColumnDelimiter(';').build())
                .setListDelimiter("|")
                .build();
        RosettaCsvMapper csvObjectMapper = (RosettaCsvMapper) RosettaObjectMapperCreator.forCSV(config).create();
        User user = User.builder()
                .setFirstName("FirstName")
                .setLastName("LastName")
                .setIdentifier("identifier")
                .setUsername("username")
                .build();

        String serialized = csvObjectMapper.writeValueAsString(user);
        String expected = "username;identifier;firstName;lastName\n"
                + "username;identifier;FirstName;LastName\n";
        assertEquals(expected, serialized);

        User roundTripped = csvObjectMapper.readValue(serialized, User.class);
        assertEquals(user.build(), roundTripped);
    }

    @Test
    void testSemicolonDialectLoadedFromInputStreamRoundTrips() throws IOException {
        String json = "{\"dialect\":{\"columnDelimiter\":\";\"},\"listDelimiter\":\"|\"}";
        RosettaCsvMapper csvObjectMapper = (RosettaCsvMapper) RosettaObjectMapperCreator
                .forCSV(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)))
                .create();
        User user = User.builder()
                .setFirstName("FirstName")
                .setLastName("LastName")
                .setIdentifier("identifier")
                .setUsername("username")
                .build();

        String serialized = csvObjectMapper.writeValueAsString(user);
        String expected = "username;identifier;firstName;lastName\n"
                + "username;identifier;FirstName;LastName\n";
        assertEquals(expected, serialized);

        User roundTripped = csvObjectMapper.readValue(serialized, User.class);
        assertEquals(user.build(), roundTripped);
    }

    @Test
    void testValueContainingTheColumnDelimiterIsQuoted() throws IOException {
        RosettaCsvMapper csvObjectMapper = RosettaCsvMapper.createCsvObjectMapper();
        User user = User.builder()
                .setFirstName("First,Name")
                .setLastName("LastName")
                .setIdentifier("identifier")
                .setUsername("username")
                .build();

        String serialized = csvObjectMapper.writeValueAsString(user);
        String expected = "username,identifier,firstName,lastName\n"
                + "username,identifier,\"First,Name\",LastName\n";
        assertEquals(expected, serialized);

        User roundTripped = csvObjectMapper.readValue(serialized, User.class);
        assertEquals(user.build(), roundTripped);
    }

    @Test
    void testValueContainingTheQuoteCharacterIsEscaped() throws IOException {
        RosettaCsvMapper csvObjectMapper = RosettaCsvMapper.createCsvObjectMapper();
        User user = User.builder()
                .setFirstName("First\"Name")
                .setLastName("LastName")
                .setIdentifier("identifier")
                .setUsername("username")
                .build();

        String serialized = csvObjectMapper.writeValueAsString(user);
        String expected = "username,identifier,firstName,lastName\n"
                + "username,identifier,\"First\"\"Name\",LastName\n";
        assertEquals(expected, serialized);

        User roundTripped = csvObjectMapper.readValue(serialized, User.class);
        assertEquals(user.build(), roundTripped);
    }

    /**
     * Customising the quote character must not introduce an escape character. If {@code escapeChar} defaulted
     * to the quote character, changing {@code quoteChar} would leave the two unequal and the mapper would
     * configure {@code "} as a real escape character, doubling every {@code "} in the data. It would still
     * round-trip through this mapper, but a conforming single-quote/no-escape reader would see {@code a""b}
     * where the value is {@code a"b}.
     */
    @Test
    void testCustomQuoteCharDoesNotEscapeTheDefaultQuoteCharacter() throws IOException {
        RosettaCSVConfiguration config = RosettaCSVConfiguration.builder()
                .setDialect(CsvDialect.builder().setQuoteChar('\'').build())
                .build();
        RosettaCsvMapper csvObjectMapper = (RosettaCsvMapper) RosettaObjectMapperCreator.forCSV(config).create();
        User user = User.builder()
                .setFirstName("First\"Name")
                .setLastName("LastName")
                .setIdentifier("identifier")
                .setUsername("username")
                .build();

        // Only the value needing them gets quotes, and the double quote is not doubled — no escape
        // character is in force.
        String serialized = csvObjectMapper.writeValueAsString(user);
        String expected = "username,identifier,firstName,lastName\n"
                + "username,identifier,'First\"Name',LastName\n";
        assertEquals(expected, serialized);

        User roundTripped = csvObjectMapper.readValue(serialized, User.class);
        assertEquals(user.build(), roundTripped);
    }

    /**
     * An explicitly configured escape character still applies: there is no default, but the capability
     * remains.
     */
    @Test
    void testExplicitEscapeCharIsHonoured() throws IOException {
        RosettaCSVConfiguration config = RosettaCSVConfiguration.builder()
                .setDialect(CsvDialect.builder().setQuoteChar('\'').setEscapeChar('\\').build())
                .build();
        RosettaCsvMapper csvObjectMapper = (RosettaCsvMapper) RosettaObjectMapperCreator.forCSV(config).create();
        User user = User.builder()
                .setFirstName("First\\Name")
                .setLastName("LastName")
                .setIdentifier("identifier")
                .setUsername("username")
                .build();

        String serialized = csvObjectMapper.writeValueAsString(user);
        assertTrue(serialized.contains("First\\\\Name"));

        User roundTripped = csvObjectMapper.readValue(serialized, User.class);
        assertEquals(user.build(), roundTripped);
    }
}
