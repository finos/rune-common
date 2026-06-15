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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.rosetta.model.lib.functions.LabelProvider;
import com.rosetta.model.lib.path.RosettaPath;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class RosettaCsvMapper extends CsvMapper  {
    private final CsvSchema defaultSchema;
    private final LabelProvider labelProvider;

    public RosettaCsvMapper() {
        this(null);
    }

    public RosettaCsvMapper(LabelProvider labelProvider) {
        this.defaultSchema = CsvSchema.emptySchema().withHeader();
        this.labelProvider = labelProvider;
    }

    @Override
    public <T> T readValue(String content, Class<T> valueType) throws JsonMappingException {
        try {
            return super.readerFor(valueType).with(defaultSchema).readValue(content, valueType);
        } catch (IOException e) {
            throw  new JsonMappingException(null,
                    String.format("IOException (of type %s): %s",
                            e.getClass().getName(),
                            ClassUtil.exceptionMessage(e)));
        }
    }

    @Override
    public <T> T readValue(URL src, Class<T> valueType) throws IOException {
        return super.readerFor(valueType).with(defaultSchema).readValue(src, valueType);
    }

    @Override
    public String writeValueAsString(Object value) throws JsonProcessingException {
        SerializationConfig config = getSerializationConfig();
        RosettaCsvObjectWriter rosettaCsvObjectWriter = new RosettaCsvObjectWriter(this, config, labelProvider);
        return rosettaCsvObjectWriter.writeValueAsString(value);
    }

    @Override
    public ObjectWriter writerWithDefaultPrettyPrinter() {
        SerializationConfig config = getSerializationConfig();
        return new RosettaCsvObjectWriter(this, config, labelProvider);
    }

    private static class RosettaCsvObjectWriter extends ObjectWriter {
        private final CsvMapper mapper;
        private final LabelProvider labelProvider;

        protected RosettaCsvObjectWriter(CsvMapper mapper, SerializationConfig config, LabelProvider labelProvider) {
            super(mapper, config);
            this.mapper = mapper;
            this.labelProvider = labelProvider;
        }

        @Override
        public String writeValueAsString(Object value) throws JsonProcessingException {
            if (labelProvider == null) {
                CsvSchema schema = mapper.schemaFor(value.getClass()).withHeader();
                return mapper.writer(schema).writeValueAsString(value);
            }
            CsvSchema schema = mapper.schemaFor(value.getClass()).withoutHeader();
            String body = mapper.writer(schema).writeValueAsString(value);
            List<String> headers = new ArrayList<>();
            for (CsvSchema.Column column : schema) {
                String name = column.getName();
                String label = labelProvider.getLabel(RosettaPath.valueOf(name));
                headers.add(label != null ? label : name);
            }
            CsvSchema headerSchema = CsvSchema.emptySchema().withoutHeader();
            String headerLine = mapper.writer(headerSchema).writeValueAsString(headers.toArray(new String[0]));
            return headerLine + body;
        }
    }

    public static RosettaCsvMapper createCsvObjectMapper() {
        return (RosettaCsvMapper) RosettaObjectMapperCreator.forCSV().create();
    }

    public static RosettaCsvMapper createCsvObjectMapper(LabelProvider labelProvider) {
        return (RosettaCsvMapper) RosettaObjectMapperCreator.forCSV(labelProvider).create();
    }
}
