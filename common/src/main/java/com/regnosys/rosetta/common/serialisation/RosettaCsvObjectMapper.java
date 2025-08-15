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
import java.io.StringReader;
import java.util.List;
import java.util.Map;

public class RosettaCsvObjectMapper extends CsvMapper  {
    private final CsvSchema defaultSchema;

    public RosettaCsvObjectMapper() {
        this.defaultSchema = CsvSchema.emptySchema().withHeader();
    }

    @Override
    public <T> T readValue(String content, Class<T> valueType) {
        // Split the CSV into lines
        String[] lines = content.split("\\R");
        if (lines.length < 2) {
            throw new IllegalArgumentException("CSV must have at least a header and one data row");
        }

        // Reuse the header from the first line
        String header = lines[0];
        String secondRow = lines[1];

        // Build CSV string containing only the header and the desired data row
        String csvToParse = header + System.lineSeparator() + secondRow;

        // Read the single object
        List<T> result;
        try {
            result = this
                    .readerFor(valueType)
                    .with(defaultSchema)
                    .<T>readValues(new StringReader(csvToParse))
                    .readAll();
        } catch (IOException e) {
            throw new IllegalStateException("", e);
        }

        //Return a single row
        return result.get(0);
    }

    public List<Map<String, String>> readCsv(String csv) throws IOException {
        try (MappingIterator<Map<String, String>> iterator = this.readerFor(Map.class).with(defaultSchema).readValues(csv)) {
            return iterator.readAll();
        }
    }

    public String writeCsv(Object value) throws IOException {
        CsvSchema schema = this.schemaFor(value.getClass()).withHeader();
        return this.writer(schema).writeValueAsString(value);
    }

    public static RosettaCsvObjectMapper createCsvObjectMapper() {
        return (RosettaCsvObjectMapper) RosettaObjectMapperCreator.forCSV().create();
    }
}
