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
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.rosetta.model.lib.functions.LabelProvider;
import com.rosetta.model.lib.path.RosettaPath;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RosettaCsvMapper extends CsvMapper  {
    private static final Logger LOGGER = LoggerFactory.getLogger(RosettaCsvMapper.class);

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
            if (labelProvider == null) {
                return super.readerFor(valueType).with(defaultSchema).readValue(content, valueType);
            }
            List<String> headerLabels = readHeaderLabels(content);
            CsvSchema labelReadSchema = buildLabelReadSchema(valueType, headerLabels);
            return super.readerFor(valueType).with(labelReadSchema).readValue(content, valueType);
        } catch (IOException e) {
            throw  new JsonMappingException(null,
                    String.format("IOException (of type %s): %s",
                            e.getClass().getName(),
                            ClassUtil.exceptionMessage(e)));
        }
    }

    @Override
    public <T> T readValue(URL src, Class<T> valueType) throws IOException {
        if (labelProvider == null) {
            return super.readerFor(valueType).with(defaultSchema).readValue(src, valueType);
        }
        String content = IOUtils.toString(src, StandardCharsets.UTF_8);
        List<String> headerLabels = readHeaderLabels(content);
        CsvSchema labelReadSchema = buildLabelReadSchema(valueType, headerLabels);
        return super.readerFor(valueType).with(labelReadSchema).readValue(content, valueType);
    }

    private List<String> readHeaderLabels(String content) throws IOException {
        try (MappingIterator<String[]> rows = super.readerFor(String[].class)
                .with(CsvSchema.emptySchema())
                .with(CsvParser.Feature.WRAP_AS_ARRAY)
                .readValues(content)) {
            if (!rows.hasNext()) {
                throw new IllegalStateException("Cannot deserialise labelled CSV: missing header row");
            }
            return Arrays.asList(rows.nextValue());
        }
    }

    private CsvSchema buildLabelReadSchema(Class<?> valueType, List<String> headerLabels) {
        CsvSchema schema = schemaFor(valueType);
        Map<String, String> labelToAttribute = new HashMap<>();
        boolean ambiguousLabels = false;
        String duplicateLabel = null;
        String duplicateAttribute = null;
        for (CsvSchema.Column column : schema) {
            String attribute = column.getName();
            String label = labelProvider.getLabel(RosettaPath.valueOf(attribute));
            String key = label != null ? label : attribute;
            String previous = labelToAttribute.putIfAbsent(key, attribute);
            if (previous != null) {
                // Two attributes share a label, so the header text cannot be resolved to a
                // single attribute. Fall back to positional binding against the canonical
                // schema order, which is the order the writer always emits columns in.
                ambiguousLabels = true;
                duplicateLabel = key;
                duplicateAttribute = attribute;
            }
        }
        if (ambiguousLabels) {
            LOGGER.warn("Ambiguous CSV label '{}' is shared by attribute '{}' and at least one other attribute of {}; "
                            + "falling back to positional binding instead of label-based binding.",
                    duplicateLabel, duplicateAttribute, valueType.getName());
            return buildPositionalReadSchema(valueType, schema, headerLabels);
        }
        CsvSchema.Builder builder = CsvSchema.builder();
        Set<String> consumed = new HashSet<>();
        for (String headerLabel : headerLabels) {
            String attribute = labelToAttribute.get(headerLabel);
            if (attribute == null) {
                throw new IllegalStateException(
                        String.format("Unknown header label '%s': no attribute of %s maps to this label",
                                headerLabel, valueType.getName()));
            }
            if (!consumed.add(attribute)) {
                throw new IllegalStateException(
                        String.format("Duplicate header label '%s' resolves to attribute '%s' "
                                        + "which is already mapped by another column",
                                headerLabel, attribute));
            }
            builder.addColumn(attribute);
        }
        return builder.build().withSkipFirstDataRow(true);
    }

    /**
     * Binds columns by position against the type's canonical schema order rather than by
     * label name. Used only when duplicate labels make name-based binding impossible.
     * Requires the header column count to match the schema so that a structurally unexpected
     * file (e.g. reordered or truncated) fails fast rather than silently mis-mapping columns.
     */
    private CsvSchema buildPositionalReadSchema(Class<?> valueType, CsvSchema schema, List<String> headerLabels) {
        if (headerLabels.size() != schema.size()) {
            throw new IllegalStateException(
                    String.format("Ambiguous labels force positional binding for %s, but the header has "
                                    + "%d column(s) while the type has %d: cannot safely map columns by position",
                            valueType.getName(), headerLabels.size(), schema.size()));
        }
        CsvSchema.Builder builder = CsvSchema.builder();
        for (CsvSchema.Column column : schema) {
            builder.addColumn(column.getName());
        }
        return builder.build().withSkipFirstDataRow(true);
    }

    //TODO: see if it's possible to use a custom serialiser so we don't have to override the writer methods
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

        //TODO: see if it's possible to use a custom serialiser so we don't have to override the writer methods
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
