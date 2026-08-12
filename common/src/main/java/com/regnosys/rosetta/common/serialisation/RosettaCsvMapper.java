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
import com.fasterxml.jackson.databind.SequenceWriter;
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
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class RosettaCsvMapper extends CsvMapper  {
    private static final Logger LOGGER = LoggerFactory.getLogger(RosettaCsvMapper.class);

    private final RosettaCSVConfiguration configuration;
    /**
     * The plain read schema: column-less, so jackson takes the column names from the header row.
     * Type-independent, hence a field. Unused when {@code hasHeader} is {@code false} — a header-less
     * read needs a type-specific column list, built per call by {@link #buildHeaderlessReadSchema}.
     */
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

    /**
     * The header style and the {@link LabelProvider} have to agree: {@link HeaderStyle#LABEL} is the one
     * style that consults a provider, and the only one that can. Either half without the other is rejected
     * rather than resolved silently, so a supplied provider is never dropped without a word.
     *
     * @throws IllegalArgumentException if {@code headerStyle} is {@code LABEL} and no provider is
     *                                  supplied, or a provider is supplied and {@code headerStyle}
     *                                  is anything else
     */
    public RosettaCsvMapper(RosettaCSVConfiguration configuration, LabelProvider labelProvider) {
        boolean labelHeaders = configuration.getHeaderStyle() == HeaderStyle.LABEL;
        if (labelHeaders && labelProvider == null) {
            throw new IllegalArgumentException(
                    "RosettaCSVConfiguration specifies headerStyle=LABEL but no LabelProvider was supplied; "
                            + "a LABEL header style has no attribute-to-label mapping to use.");
        }
        if (!labelHeaders && labelProvider != null) {
            throw new IllegalArgumentException(
                    "A LabelProvider was supplied but RosettaCSVConfiguration specifies headerStyle="
                            + configuration.getHeaderStyle() + ", which never consults one, so the labels would "
                            + "be silently dropped. Set headerStyle=LABEL, or supply no LabelProvider.");
        }
        this.configuration = configuration;
        this.labelProvider = labelProvider;
        this.defaultSchema = dialectSchema(CsvSchema.emptySchema().withHeader());
    }

    private static RosettaCSVConfiguration configurationFor(LabelProvider labelProvider) {
        if (labelProvider == null) {
            return RosettaCSVConfiguration.EMPTY;
        }
        return RosettaCSVConfiguration.builder().setHeaderStyle(HeaderStyle.LABEL).build();
    }

    @Override
    public <T> T readValue(String content, Class<T> valueType) throws JsonMappingException {
        try {
            return readValueFromContent(content, valueType);
        } catch (IOException e) {
            throw  new JsonMappingException(null,
                    String.format("IOException (of type %s): %s",
                            e.getClass().getName(),
                            ClassUtil.exceptionMessage(e)));
        }
    }

    @Override
    public <T> T readValue(URL src, Class<T> valueType) throws IOException {
        if (canStream(valueType)) {
            return super.readerFor(valueType).with(defaultSchema).readValue(src, valueType);
        }
        String content = IOUtils.toString(src, StandardCharsets.UTF_8);
        return readValueFromContent(content, valueType);
    }

    /**
     * Whether a {@link URL} can be handed straight to jackson rather than being buffered into a
     * {@code String} first. Three things need the whole document up front: reading the header row to
     * resolve labels; the list-element null-token pre-pass, for a type that has a multi-cardinality
     * attribute for a null token to appear inside; and a header-less read, which has to measure the first
     * row's width before binding it (see {@link #buildHeaderlessReadSchema}). A type of scalars only, read
     * with a header, still streams.
     *
     * <p>Takes {@code valueType} because the second condition is a property of the type, not of the
     * configuration. Not merely a performance question: the streaming path skips
     * {@link #stripListElementsMeaningAbsent}, so a list cell that needs stripping would reach jackson
     * intact and fail deep inside Guava's null-rejecting {@code ImmutableList}.</p>
     */
    private boolean canStream(Class<?> valueType) {
        return configuration.getHeaderStyle() != HeaderStyle.LABEL
                && configuration.isHasHeader()
                && multiValuedAttributeNames(valueType).isEmpty();
    }

    private <T> T readValueFromContent(String content, Class<T> valueType) throws IOException {
        if (configuration.getHeaderStyle() == HeaderStyle.LABEL) {
            List<String> headerLabels = readFirstRow(content);
            if (headerLabels.isEmpty()) {
                throw new IllegalStateException("Cannot deserialise labelled CSV: missing header row");
            }
            CsvSchema labelReadSchema = buildLabelReadSchema(valueType, headerLabels);
            String normalized = stripListElementsMeaningAbsent(
                    content, valueType, columnNames(labelReadSchema));
            return super.readerFor(valueType).with(labelReadSchema).readValue(normalized, valueType);
        }
        if (!configuration.isHasHeader()) {
            CsvSchema positionalSchema = buildHeaderlessReadSchema(valueType, content);
            // The column names have to be handed over here, unlike on the path below: with no header
            // row, reading the first row yields data, not attribute names, and the pre-pass would
            // match none of them and strip nothing.
            String normalized = stripListElementsMeaningAbsent(
                    content, valueType, columnNames(positionalSchema));
            return super.readerFor(valueType).with(positionalSchema).readValue(normalized, valueType);
        }
        // No column names to hand over: the header row of a non-LABEL file holds attribute names
        // already, so the pre-pass reads them itself — and only if it has anything to strip.
        String normalized = stripListElementsMeaningAbsent(content, valueType, null);
        return super.readerFor(valueType).with(defaultSchema).readValue(normalized, valueType);
    }

    /**
     * Binds a header-less file's columns by position, against the type's attribute declaration order —
     * the one order every other path in this class uses, so a header-less write and a header-less read
     * agree by construction. The explicit column list is what makes this work: an
     * {@code emptySchema().withoutHeader()} has no columns, so jackson emits an array and nothing binds to
     * the bean.
     *
     * <p>Requires the first row's width to match the type's column count exactly. With a header row a
     * structurally unexpected file announces itself; with none, a short row silently leaves its trailing
     * attributes absent, which for a mandatory attribute becomes a cardinality violation somewhere later,
     * or plausible-looking data. Jackson throws on a row that is too <em>wide</em>, but that is checked
     * here as well so both directions of mismatch report the same thing, naming the type and both
     * counts.</p>
     *
     * <p><b>The first row only, which is the whole document this call binds.</b> A {@code readValue} binds
     * one value from one row, so this is not a validation of the file: a multi-row header-less document
     * passes on the strength of row one, and a caller that iterates rows itself gets no width check on rows
     * two onward.</p>
     *
     * @param content the whole document — needed to measure the first row, which is why a header-less
     *                read cannot stream ({@link #canStream})
     */
    private CsvSchema buildHeaderlessReadSchema(Class<?> valueType, String content) throws IOException {
        CsvSchema schema = dialectSchema(schemaInDeclarationOrder(valueType).withoutHeader());
        List<String> firstRow = readFirstRow(content);
        // An empty document has no row to measure. Left to jackson, whose "No content to map due to
        // end-of-input" says it better than a column-count complaint about a file holding no columns.
        if (!firstRow.isEmpty() && firstRow.size() != schema.size()) {
            throw new IllegalStateException(
                    String.format("Cannot deserialise header-less CSV: the first row has %d column(s) while %s has "
                                    + "%d attribute(s). With no header row, columns bind by position, so a row of "
                                    + "the wrong width cannot be mapped without silently shifting or dropping "
                                    + "values. Expected columns, in order: %s",
                            firstRow.size(), valueType.getName(), schema.size(), columnNames(schema)));
        }
        return schema;
    }

    private static List<String> columnNames(CsvSchema schema) {
        List<String> names = new ArrayList<>();
        for (CsvSchema.Column column : schema) {
            names.add(column.getName());
        }
        return names;
    }

    /**
     * The first row of {@code content}, read raw, or an <b>empty list</b> if the document holds no row at
     * all. What that row <em>means</em> is the caller's business: header labels on the labelled path,
     * attribute names on the plain path, and a data row when {@code hasHeader} is {@code false}, where only
     * its width is of interest.
     *
     * <p>Empty rather than an exception because the callers disagree about what a missing first row means.
     * The labelled path treats it as fatal and says so; elsewhere jackson's own message from the read that
     * follows describes an empty document better than anything thrown from here.</p>
     */
    private List<String> readFirstRow(String content) throws IOException {
        try (MappingIterator<String[]> rows = super.readerFor(String[].class)
                .with(dialectSchema(CsvSchema.emptySchema()))
                .with(CsvParser.Feature.WRAP_AS_ARRAY)
                .readValues(content)) {
            if (!rows.hasNext()) {
                return Collections.emptyList();
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
                // single attribute. Fall back to positional binding against attribute
                // declaration order, which is the order the writer always emits columns in.
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
     * Binds columns by position against the type's attribute declaration order rather than by label name.
     * Used only when duplicate labels make name-based binding impossible. Requires the header column count
     * to match the schema, so a reordered or truncated file fails fast rather than silently mis-mapping
     * columns — the same requirement {@link #buildHeaderlessReadSchema} imposes, differing only in that the
     * row measured there is data rather than a header.
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
     * Stamps the configured {@link CsvDialect} (column separator, quote character, escape character),
     * the canonical null token, and the list delimiter onto a schema this class produces. Every schema
     * built anywhere in this class must be passed through here — a call site that forgets produces a
     * mapper that reads and writes differently.
     */
    private CsvSchema dialectSchema(CsvSchema schema) {
        CsvDialect dialect = configuration.getDialect();
        CsvSchema.Builder builder = schema.rebuild()
                .setColumnSeparator(dialect.getColumnDelimiter())
                .setQuoteChar(dialect.getQuoteChar())
                // Schema-wide rather than per column: jackson's CsvGenerator recognises a multi-valued
                // property by the writeStartArray()/writeEndArray() calls its own bean serialiser makes,
                // regardless of the column's declared ColumnType, and always consults this one
                // schema-level separator to join the elements into the cell.
                .setArrayElementSeparator(configuration.getListDelimiter());
        if (dialect.getEscapeChar() == null) {
            // No escape character — RFC 4180's doubled quote, and the default. See CsvDialect's javadoc
            // for why this is null rather than the quote character.
            builder.disableEscapeChar();
        } else {
            builder.setEscapeChar(dialect.getEscapeChar());
        }
        // On read, a cell exactly matching this is reported as VALUE_NULL by the parser itself, before any
        // type-specific (number/date/boolean) text parsing; on write, it is the token an absent value is
        // written back as. Works for every column type because it is the parser, not a bean deserialiser,
        // that recognises it.
        //
        // The write half depends on RosettaObjectMapperCreator giving the CSV path ALWAYS serialisation
        // inclusion: an absent attribute has to reach the generator as a null for this null value to be
        // consulted. Under an omitting inclusion the generator leaves the column empty and this setting
        // has no effect on write, so the two must stay paired.
        builder.setNullValue(configuration.getNullToken());
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

    /**
     * {@code schema}'s columns in {@code order}, <b>with the column set unchanged</b>: a column jackson
     * knows about that {@code order} does not mention is kept, appended in {@code schemaFor} order after
     * the ordered ones.
     *
     * <p>That last part is load-bearing, not defensive. {@code order} comes from the generated
     * {@code process} visitor, which does not report every property jackson serialises — a
     * metadata-annotated attribute ({@code [metadata scheme]}, {@code [metadata id]}) is generated as a
     * {@code FieldWithMeta*} wrapper and so arrives at {@code processRosetta} rather than
     * {@code processBasic}. {@link DeclarationOrderCollector} records those, so they keep their declaration
     * position; a property no collector knows about at all would otherwise be dropped from the schema
     * silently, and jackson then fails on writing a property with no column ({@code Unrecognized column})
     * or rejects a file whose width no longer matches.</p>
     */
    private static CsvSchema reorderColumns(CsvSchema schema, List<String> order) {
        CsvSchema.Builder builder = CsvSchema.builder();
        Set<String> placed = new HashSet<>();
        for (String attribute : order) {
            CsvSchema.Column column = schema.column(attribute);
            if (column != null) {
                builder.addColumn(column);
                placed.add(attribute);
            }
        }
        for (CsvSchema.Column column : schema) {
            if (!placed.contains(column.getName())) {
                builder.addColumn(column);
            }
        }
        return builder.build();
    }

    /**
     * Recovers attribute declaration order from the generated {@code process(RosettaPath, Processor)}
     * visitor, which walks attributes in the order they appear in the {@code .rosetta} source.
     * {@code @RosettaAttribute}/{@code @RuneAttribute} carry only a name, no index, so the visitor is
     * the only source of this order.
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
     * Rejects a list element this mapper could write but could not read back, at the point the bad value is
     * known, rather than leaving the writer to emit a file its own reader refuses. Two kinds cannot
     * round-trip:
     *
     * <ul>
     *   <li>an element <b>containing the configured list delimiter</b>. Jackson's array handling has no
     *       escape for it (RFC 4180 quoting is a cell-level concern, already consumed by the time the cell
     *       is split on this delimiter), so a two-element list containing it would be written and read back
     *       as three; and</li>
     *   <li>an element <b>equal to the configured null token</b> — the empty string under the default
     *       {@code nullToken=""}. Such an element is indistinguishable from one meaning "absent", and
     *       {@link #stripListElementsMeaningAbsent} drops those on read, so writing it would silently
     *       shorten the list — for a {@code (1..*)} attribute, a cardinality violation.</li>
     * </ul>
     *
     * <p>The second check uses <b>the same token</b> as {@link #stripListElementsMeaningAbsent}, so the
     * writer refuses precisely the element the reader would drop. <b>The two must be changed
     * together.</b></p>
     *
     * <p>Only {@link RosettaModelObject} values are checked — a plain POJO (which
     * {@link #schemaInDeclarationOrder(Object)} also accommodates) has no {@code process} visitor to
     * walk.</p>
     *
     * @throws IllegalArgumentException naming the attribute and the offending value
     */
    private void rejectListElementsThatCannotRoundTrip(Object value) {
        if (!(value instanceof RosettaModelObject)) {
            return;
        }
        RosettaModelObject instance = (RosettaModelObject) value;
        instance.process(RosettaPath.valueOf(instance.getType().getSimpleName()),
                new ListElementCollisionDetector(configuration.getListDelimiter(), configuration.getNullToken()));
    }

    /**
     * Collects the attribute names {@code process} visits, in visitation (declaration) order.
     *
     * <p>Records what {@code processRosetta} visits as well as what {@code processBasic} does, because a
     * <b>metadata-annotated</b> attribute is not visited as basic: {@code [metadata scheme]} or
     * {@code [metadata id]} on a {@code string} is generated as a {@code FieldWithMetaString} wrapper,
     * which is a {@link RosettaModelObject}, so the visitor routes it to {@code processRosetta}. Such an
     * attribute is still a column jackson serialises, so it has to keep its declaration position.</p>
     *
     * <p>Recursion is declined in both cases: only the attribute's own position is wanted, never the fields
     * inside a wrapper or a complex attribute, which are not columns. A genuinely complex attribute is
     * recorded too, harmlessly — {@link #reorderColumns} skips a name it does not find among
     * {@code schemaFor}'s columns, and jackson is what refuses to write a nested object into a cell.</p>
     */
    private static final class DeclarationOrderCollector implements Processor {
        private final List<String> attributeNames = new ArrayList<>();

        @Override
        public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path, Class<? extends R> rosettaType,
                R instance, RosettaModelObject parent, AttributeMeta... metas) {
            attributeNames.add(path.getElement().getPath());
            return false;
        }

        @Override
        public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path, Class<? extends R> rosettaType,
                List<? extends R> instance, RosettaModelObject parent, AttributeMeta... metas) {
            attributeNames.add(path.getElement().getPath());
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

    /**
     * Walks a value's simple attributes looking for a multi-valued one holding an element that
     * cannot round-trip, throwing as soon as it finds one. Declines to recurse into a complex
     * attribute for the same reason {@link DeclarationOrderCollector} does: tabular CSV types have
     * none.
     */
    private static final class ListElementCollisionDetector implements Processor {
        private final String listDelimiter;
        private final String nullToken;

        /**
         * @param nullToken the configured null token — the exact value
         *                  {@link #stripListElementsMeaningAbsent} drops on read
         */
        private ListElementCollisionDetector(String listDelimiter, String nullToken) {
            this.listDelimiter = listDelimiter;
            this.nullToken = nullToken;
        }

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
            // A single-valued attribute serialises to one whole cell; the list delimiter has no
            // special meaning there, so there is nothing to reject.
        }

        @Override
        public <T> void processBasic(RosettaPath path, Class<? extends T> rosettaType, Collection<? extends T> instance,
                RosettaModelObject parent, AttributeMeta... metas) {
            if (instance == null) {
                return;
            }
            for (T element : instance) {
                if (element == null) {
                    continue;
                }
                String text = String.valueOf(element);
                if (text.contains(listDelimiter)) {
                    throw new IllegalArgumentException(String.format(
                            "Cannot serialise attribute '%s' to CSV: element '%s' contains the configured list "
                                    + "delimiter '%s'. A list element containing the list delimiter cannot "
                                    + "round-trip — there is no escape for a delimiter inside a list element — so "
                                    + "either change the value or configure a listDelimiter that cannot occur in it.",
                            path.getElement().getPath(), element, listDelimiter));
                }
                if (nullToken.equals(text)) {
                    throw new IllegalArgumentException(String.format(
                            "Cannot serialise attribute '%s' to CSV: element '%s' is the configured null token, so it "
                                    + "would be written as a list element indistinguishable from an absent value and "
                                    + "dropped when read back, silently shortening the list. Either change the value "
                                    + "or reconfigure nullToken so it does not collide.",
                            path.getElement().getPath(), text));
                }
            }
        }

        @Override
        public Report report() {
            return null;
        }
    }

    /**
     * Removes, from every cell bound to a multi-cardinality attribute, each list element equal to the
     * configured null token — so {@code "EUR;"} reads as {@code [EUR]} and {@code "EUR;;USD"} as
     * {@code [EUR, USD]}.
     *
     * <p>Rune has no representation for a hole inside a list — the generated immutable rejects a
     * {@code null} element outright — so dropping it applies the whole-cell rule one level down: a cell
     * meaning "absent" makes its attribute absent, so an element meaning "absent" makes its element
     * absent.</p>
     *
     * <p>A cell that splits into a <b>single</b> element is left untouched: that is the whole-cell case,
     * which belongs to the schema's own null-value handling. A cell whose <em>every</em> element is
     * stripped becomes empty, which reads back as an absent list under any configuration.</p>
     *
     * <p>Skips entirely — returning {@code content} itself rather than a re-rendered copy — when
     * {@code valueType} has no multi-cardinality attribute, or when no cell needed stripping. That first
     * guard runs <b>before</b> the header row is read, which is why the header is read here rather than
     * passed in by the plain-path caller: an argument would be evaluated first and the guard could no
     * longer prevent the work.</p>
     *
     * @param columnAttributeNames the attribute each column binds to, in column order, or {@code null} to
     *                             read them from the header row. Only the plain header-bearing path passes
     *                             {@code null}, its header being attribute names already. The labelled path
     *                             supplies them because its header holds labels; the header-less path
     *                             because its first row is data, so reading it would match no attribute
     * @return {@code content} with those elements removed, or {@code content} itself if none were
     */
    private String stripListElementsMeaningAbsent(String content, Class<?> valueType,
            List<String> columnAttributeNames) throws IOException {
        Set<String> multiValuedAttributes = multiValuedAttributeNames(valueType);
        if (multiValuedAttributes.isEmpty()) {
            return content;
        }
        String nullToken = configuration.getNullToken();
        List<String> columns = columnAttributeNames != null ? columnAttributeNames : readFirstRow(content);
        CsvSchema rawSchema = dialectSchema(CsvSchema.emptySchema());
        List<String[]> rows = new ArrayList<>();
        boolean stripped = false;
        try (MappingIterator<String[]> it = super.readerFor(String[].class)
                .with(rawSchema)
                .with(CsvParser.Feature.WRAP_AS_ARRAY)
                .readValues(content)) {
            boolean isHeaderRow = configuration.isHasHeader();
            while (it.hasNext()) {
                String[] row = it.nextValue();
                for (int i = 0; i < row.length; i++) {
                    if (row[i] == null) {
                        // Restored to text: the raw reader maps a cell equal to nullToken to Java null (the
                        // schema's null value), and jackson's CSV generator omits a null element from this
                        // column-less schema rather than writing an empty field, shifting every later
                        // column one place left. Not on its own a reason to re-emit, so no flag.
                        row[i] = nullToken;
                        continue;
                    }
                    if (isHeaderRow || i >= columns.size() || !multiValuedAttributes.contains(columns.get(i))) {
                        continue;
                    }
                    String cell = withoutElementsMatching(row[i], nullToken);
                    if (!cell.equals(row[i])) {
                        row[i] = cell;
                        stripped = true;
                    }
                }
                isHeaderRow = false;
                rows.add(row);
            }
        }
        if (!stripped) {
            return content;
        }
        StringWriter out = new StringWriter();
        try (SequenceWriter seq = super.writer(rawSchema).writeValues(out)) {
            for (String[] row : rows) {
                seq.write(row);
            }
        }
        return out.toString();
    }

    /**
     * {@code cell} with every list element equal to {@code nullToken} removed, or {@code cell} itself
     * if it holds a single element or none matched. A cell whose every element matched becomes the
     * empty string, which reads back as an absent list — the same value an empty cell gives.
     */
    private String withoutElementsMatching(String cell, String nullToken) {
        String listDelimiter = configuration.getListDelimiter();
        String[] elements = cell.split(Pattern.quote(listDelimiter), -1);
        if (elements.length < 2) {
            return cell;
        }
        List<String> kept = new ArrayList<>(elements.length);
        for (String element : elements) {
            if (!nullToken.equals(element)) {
                kept.add(element);
            }
        }
        if (kept.size() == elements.length) {
            return cell;
        }
        return String.join(listDelimiter, kept);
    }

    private Set<String> multiValuedAttributeNames(Class<?> valueType) {
        if (!RosettaModelObject.class.isAssignableFrom(valueType)) {
            return Collections.emptySet();
        }
        MultiValuedAttributeCollector collector = new MultiValuedAttributeCollector();
        RosettaModelObject instance = emptyInstance(valueType);
        instance.process(RosettaPath.valueOf(instance.getType().getSimpleName()), collector);
        return collector.attributeNames;
    }

    /**
     * Collects the attribute names {@code process} visits via its multi-valued overload — the
     * declaration-order counterpart of {@link DeclarationOrderCollector}, scoped to only the
     * attributes a CSV list column applies to.
     */
    private static final class MultiValuedAttributeCollector implements Processor {
        private final Set<String> attributeNames = new HashSet<>();

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
            // Single-valued: not a list column, nothing to collect.
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
            mapper.rejectListElementsThatCannotRoundTrip(value);
            CsvSchema schemaInOrder = mapper.schemaInDeclarationOrder(value);
            if (mapper.configuration.getHeaderStyle() != HeaderStyle.LABEL) {
                // hasHeader describes the file on both sides, so it suppresses the header row on write as
                // well as expecting none on read. Safe because there is one canonical column order across
                // every path in this class, so a header-less write and a header-less read agree by
                // construction. The LABEL branch below cannot reach hasHeader=false —
                // RosettaCSVConfiguration refuses that combination.
                CsvSchema schema = mapper.configuration.isHasHeader()
                        ? mapper.dialectSchema(schemaInOrder.withHeader())
                        : mapper.dialectSchema(schemaInOrder.withoutHeader());
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
