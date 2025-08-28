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
import csv.test.user.User;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

        String expected = "firstName,identifier,lastName,username\n" +
                "FirstName,identifier,LastName,username\n";
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
}
