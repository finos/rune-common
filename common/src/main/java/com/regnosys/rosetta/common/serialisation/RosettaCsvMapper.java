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
import com.regnosys.rosetta.common.serialisation.csv.config.CsvDialect;
import com.regnosys.rosetta.common.serialisation.csv.config.HeaderStyle;
import com.regnosys.rosetta.common.serialisation.csv.config.RosettaCSVConfiguration;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.functions.LabelProvider;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.Processor;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class RosettaCsvMapper extends CsvMapper  {
    private static final Logger LOGGER = LoggerFactory.getLogger(RosettaCsvMapper.class);

    private final RosettaCSVConfiguration configuration;
    private final CsvSchema defaultSchema;
    private final LabelProvider labelProvider;

    public RosettaCsvMapper() {
        this(RosettaCSVConfiguration.EMPTY, null);
    }

    /**
     * Compatibility shim for the pre-configuration constructor: {@code headerStyle} is derived from
     * whether a {@link LabelProvider} is supplied (present -&gt; {@code LABEL}, absent -&gt;
     * {@code ATTRIBUTE_NAME}), preserving the original behaviour exactly.
     */
    public RosettaCsvMapper(LabelProvider labelProvider) {
        this(configurationFor(labelProvider), labelProvider);
    }

    public RosettaCsvMapper(RosettaCSVConfiguration configuration, LabelProvider labelProvider) {
        if (configuration.getHeaderStyle() == HeaderStyle.LABEL && labelProvider == null) {
            throw new IllegalArgumentException(
                    "RosettaCSVConfiguration specifies headerStyle=LABEL but no LabelProvider was supplied; "
                            + "a LABEL header style has no attribute-to-label mapping to use.");
        }
        this.configuration = configuration;
        this.labelProvider = labelProvider;
        this.defaultSchema = dialectSchema(CsvSchema.emptySchema().withHeader());
    }

    private static RosettaCSVConfiguration configurationFor(LabelProvider labelProvider) {
        if (labelProvider == null) {
            return RosettaCSVConfiguration.EMPTY;
        }
        return new RosettaCSVConfiguration(Optional.empty(), Optional.of(HeaderStyle.LABEL),
                Optional.empty(), Optional.empty(), Optional.empty());
    }

    @Override
    public <T> T readValue(String content, Class<T> valueType) throws JsonMappingException {
        try {
            if (configuration.getHeaderStyle() != HeaderStyle.LABEL) {
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
        if (configuration.getHeaderStyle() != HeaderStyle.LABEL) {
            return super.readerFor(valueType).with(defaultSchema).readValue(src, valueType);
        }
        String content = IOUtils.toString(src, StandardCharsets.UTF_8);
        List<String> headerLabels = readHeaderLabels(content);
        CsvSchema labelReadSchema = buildLabelReadSchema(valueType, headerLabels);
        return super.readerFor(valueType).with(labelReadSchema).readValue(content, valueType);
    }

    private List<String> readHeaderLabels(String content) throws IOException {
        try (MappingIterator<String[]> rows = super.readerFor(String[].class)
                .with(dialectSchema(CsvSchema.emptySchema()))
                .with(CsvParser.Feature.WRAP_AS_ARRAY)
                .readValues(content)) {
            if (!rows.hasNext()) {
                throw new IllegalStateException("Cannot deserialise labelled CSV: missing header row");
            }
            return Arrays.asList(rows.nextValue());
        }
    }

    private CsvSchema buildLabelReadSchema(Class<?> valueType, List<String> headerLabels) {
        CsvSchema schema = schemaInDeclarationOrder(valueType);
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
        return dialectSchema(builder.build().withSkipFirstDataRow(true));
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
        return dialectSchema(builder.build().withSkipFirstDataRow(true));
    }

    /**
     * Stamps the configured {@link CsvDialect} (column separator, quote character, escape character)
     * onto a schema this class produces. Every schema built anywhere in this class must be passed
     * through here — a call site that forgets produces a mapper that reads and writes differently.
     */
    private CsvSchema dialectSchema(CsvSchema schema) {
        CsvDialect dialect = configuration.getDialect();
        CsvSchema.Builder builder = schema.rebuild()
                .setColumnSeparator(dialect.getColumnDelimiter())
                .setQuoteChar(dialect.getQuoteChar());
        if (dialect.getEscapeChar() == dialect.getQuoteChar()) {
            // RFC 4180 has no distinct escape character, only the doubled quote: see CsvDialect's
            // javadoc. Translating that into CsvSchema means no explicit escape character at all.
            builder.disableEscapeChar();
        } else {
            builder.setEscapeChar(dialect.getEscapeChar());
        }
        return builder.build();
    }

    /**
     * The schema for {@code valueType}, with columns in model attribute declaration order rather
     * than {@code schemaFor}'s alphabetical default. Column types (string/number/boolean/...) still
     * come from {@code schemaFor}; only the order is overridden.
     *
     * <p>{@link RosettaCsvMapper} is a general-purpose {@code CsvMapper} — nothing stops a caller
     * writing or reading a plain POJO that is not a {@link RosettaModelObject} and so has no
     * declaration order to recover. Such a type gets {@code schemaFor}'s natural order, unchanged.
     */
    private CsvSchema schemaInDeclarationOrder(Class<?> valueType) {
        CsvSchema schema = schemaFor(valueType);
        if (!RosettaModelObject.class.isAssignableFrom(valueType)) {
            return schema;
        }
        return reorderColumns(schema, declarationOrder(valueType));
    }

    private CsvSchema schemaInDeclarationOrder(Object value) {
        CsvSchema schema = schemaFor(value.getClass());
        if (!(value instanceof RosettaModelObject)) {
            return schema;
        }
        return reorderColumns(schema, declarationOrder((RosettaModelObject) value));
    }

    private static CsvSchema reorderColumns(CsvSchema schema, List<String> order) {
        CsvSchema.Builder builder = CsvSchema.builder();
        for (String attribute : order) {
            builder.addColumn(schema.column(attribute));
        }
        return builder.build();
    }

    /**
     * Recovers attribute declaration order from the generated {@code process(RosettaPath, Processor)}
     * visitor, which walks attributes in the order they appear in the {@code .rosetta} source.
     * {@code @RosettaAttribute}/{@code @RuneAttribute} carry only a name, no index, so the visitor is
     * the only source of this order. See STORY-1932 §3.4(B).
     */
    private List<String> declarationOrder(RosettaModelObject instance) {
        DeclarationOrderCollector collector = new DeclarationOrderCollector();
        instance.process(RosettaPath.valueOf(instance.getType().getSimpleName()), collector);
        return collector.attributeNames;
    }

    private List<String> declarationOrder(Class<?> valueType) {
        return declarationOrder(emptyInstance(valueType));
    }

    /**
     * An empty instance of a Rosetta-generated model interface, used only to recover attribute
     * declaration order via {@link RosettaModelObject#process}. Requires the static {@code builder()}
     * method every such interface declares — not an implementation or builder class.
     */
    private static RosettaModelObject emptyInstance(Class<?> valueType) {
        try {
            return (RosettaModelObject) valueType.getMethod("builder").invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Cannot determine CSV column declaration order for " + valueType.getName()
                            + ": no static builder() method found. RosettaCsvMapper requires a Rosetta-generated "
                            + "model interface, not an implementation or builder class.", e);
        }
    }

    /**
     * Collects the attribute names {@code process} visits, in visitation (declaration) order. Tabular
     * CSV types have only simple attributes, so {@code processRosetta} is never expected to fire; it
     * declines to recurse rather than silently omitting a complex attribute from the order.
     */
    private static final class DeclarationOrderCollector implements Processor {
        private final List<String> attributeNames = new ArrayList<>();

        @Override
        public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path, Class<? extends R> rosettaType,
                R instance, RosettaModelObject parent, AttributeMeta... metas) {
            return false;
        }

        @Override
        public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path, Class<? extends R> rosettaType,
                List<? extends R> instance, RosettaModelObject parent, AttributeMeta... metas) {
            return false;
        }

        @Override
        public <T> void processBasic(RosettaPath path, Class<? extends T> rosettaType, T instance,
                RosettaModelObject parent, AttributeMeta... metas) {
            attributeNames.add(path.getElement().getPath());
        }

        @Override
        public <T> void processBasic(RosettaPath path, Class<? extends T> rosettaType, Collection<? extends T> instance,
                RosettaModelObject parent, AttributeMeta... metas) {
            attributeNames.add(path.getElement().getPath());
        }

        @Override
        public Report report() {
            return null;
        }
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
        private final RosettaCsvMapper mapper;
        private final LabelProvider labelProvider;

        protected RosettaCsvObjectWriter(RosettaCsvMapper mapper, SerializationConfig config, LabelProvider labelProvider) {
            super(mapper, config);
            this.mapper = mapper;
            this.labelProvider = labelProvider;
        }

        //TODO: see if it's possible to use a custom serialiser so we don't have to override the writer methods
        @Override
        public String writeValueAsString(Object value) throws JsonProcessingException {
            CsvSchema schemaInOrder = mapper.schemaInDeclarationOrder(value);
            if (mapper.configuration.getHeaderStyle() != HeaderStyle.LABEL) {
                CsvSchema schema = mapper.dialectSchema(schemaInOrder.withHeader());
                return mapper.writer(schema).writeValueAsString(value);
            }
            CsvSchema schema = mapper.dialectSchema(schemaInOrder.withoutHeader());
            String body = mapper.writer(schema).writeValueAsString(value);
            List<String> headers = new ArrayList<>();
            for (CsvSchema.Column column : schema) {
                String name = column.getName();
                String label = labelProvider.getLabel(RosettaPath.valueOf(name));
                headers.add(label != null ? label : name);
            }
            CsvSchema headerSchema = mapper.dialectSchema(CsvSchema.emptySchema().withoutHeader());
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
