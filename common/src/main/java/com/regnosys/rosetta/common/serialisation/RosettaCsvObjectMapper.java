package com.regnosys.rosetta.common.serialisation;

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

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class RosettaCsvObjectMapper extends CsvMapper  {
    private final CsvSchema defaultSchema;

    public RosettaCsvObjectMapper() {
        this.defaultSchema = CsvSchema.emptySchema().withHeader();
    }

    public List<Map<String, String>> readCsv(String csv) throws IOException {
        try (MappingIterator<Map<String, String>> iterator = this.readerFor(Map.class).with(defaultSchema).readValues(csv)) {
            return iterator.readAll();
        }
    }

    public String writeCsv(Object value) throws IOException {
        return this.writer(defaultSchema).writeValueAsString(value);
    }

    public static RosettaCsvObjectMapper createCsvObjectMapper() {
        return (RosettaCsvObjectMapper) RosettaObjectMapperCreator.forCSV().create();
    }
}
