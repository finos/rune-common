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
     * The header style and the {@link LabelProvider} have to agree: {@link HeaderStyle#LABEL} is the
     * one style that consults a provider, and it is the only style that can. Either half without the
     * other is rejected here rather than resolved silently — a provider that no header style will
     * ever call would be dropped without a word, which is the failure this constructor exists to
     * make impossible.
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
     * resolve labels, the extra-null-token pre-pass, and the list-element null-token pre-pass — the
     * last one only for a type that actually has a multi-cardinality attribute for a null token to
     * appear inside. A type of scalars only, read under the default configuration, still streams.
     *
     * <p>Takes {@code valueType} rather than reading the configuration alone because the third
     * condition is a property of the type, not of the configuration. Getting this wrong is not a
     * performance question: the streaming path skips
     * {@link #stripListElementsMatchingTheCanonicalNullToken}, so a list cell that needs stripping
     * would instead reach jackson intact and fail deep inside Guava's null-rejecting
     * {@code ImmutableList}.</p>
     */
    private boolean canStream(Class<?> valueType) {
        if (configuration.getHeaderStyle() == HeaderStyle.LABEL || configuration.getNullTokens().size() > 1) {
            return false;
        }
        return configuration.getNullTokens().isEmpty() || multiValuedAttributeNames(valueType).isEmpty();
    }

    private <T> T readValueFromContent(String content, Class<T> valueType) throws IOException {
        String normalized = normalizeExtraNullTokens(content);
        if (configuration.getHeaderStyle() != HeaderStyle.LABEL) {
            // No column names to hand over: the header row of a non-LABEL file holds attribute names
            // already, so the pre-pass reads them itself — and only if it has anything to strip.
            normalized = stripListElementsMatchingTheCanonicalNullToken(normalized, valueType, null);
            return super.readerFor(valueType).with(defaultSchema).readValue(normalized, valueType);
        }
        List<String> headerLabels = readHeaderLabels(normalized);
        if (headerLabels.isEmpty()) {
            throw new IllegalStateException("Cannot deserialise labelled CSV: missing header row");
        }
        CsvSchema labelReadSchema = buildLabelReadSchema(valueType, headerLabels);
        normalized = stripListElementsMatchingTheCanonicalNullToken(
                normalized, valueType, columnNames(labelReadSchema));
        return super.readerFor(valueType).with(labelReadSchema).readValue(normalized, valueType);
    }

    private static List<String> columnNames(CsvSchema schema) {
        List<String> names = new ArrayList<>();
        for (CsvSchema.Column column : schema) {
            names.add(column.getName());
        }
        return names;
    }

    /**
     * Rewrites every cell matching one of {@code nullTokens.subList(1, size)} to the canonical null
     * token (index 0 of {@code nullTokens}, the one {@link #dialectSchema} stamps onto every schema
     * and therefore the only one the read path natively recognises). {@code CsvSchema} carries
     * exactly one null-value token, so a configuration naming more than one needs this pre-pass for
     * the rest; a no-op when zero or one token is configured, since there is nothing beyond what the
     * schema already recognises.
     *
     * <p>Reuses the mapper's own raw row reader/writer rather than hand-rolling CSV escaping, so a
     * quoted field containing the delimiter or quote character round-trips exactly as the dialect
     * defines, before and after substitution.</p>
     *
     * <p><b>Every cell is re-rendered as a non-null {@code String}, never as a Java {@code null}.</b>
     * The raw reader is driven by a column-less {@code emptySchema()}, and jackson's CSV generator
     * <em>omits</em> a null element from such a schema rather than writing an empty field — measured:
     * a row {@code {a, null, b}} renders as {@code a,b}, one column short. Since the raw reader
     * itself maps any cell equal to the canonical token to Java {@code null} (the schema's null
     * value), leaving those nulls in place would drop that column and shift every column after it
     * one place left, silently binding each attribute to its neighbour's value. Substituting the
     * canonical token's own text keeps the row's arity intact and re-reads identically.</p>
     *
     * <p>The header row is exempt from <em>token</em> substitution: a column whose name happens to
     * match a null token is still a column name, not an absent value, and rewriting it would break
     * the label lookup that reads this same row afterwards. It is not exempt from the null-to-text
     * restoration above, which preserves the header's own arity for exactly the same reason.</p>
     */
    private String normalizeExtraNullTokens(String content) throws IOException {
        List<String> nullTokens = configuration.getNullTokens();
        if (nullTokens.size() <= 1) {
            return content;
        }
        String canonicalNullToken = nullTokens.get(0);
        Set<String> extraNullTokens = new HashSet<>(nullTokens.subList(1, nullTokens.size()));
        CsvSchema rawSchema = dialectSchema(CsvSchema.emptySchema());
        List<String[]> rows = new ArrayList<>();
        try (MappingIterator<String[]> it = super.readerFor(String[].class)
                .with(rawSchema)
                .with(CsvParser.Feature.WRAP_AS_ARRAY)
                .readValues(content)) {
            boolean firstRowIsHeader = configuration.isHasHeader();
            while (it.hasNext()) {
                String[] row = it.nextValue();
                boolean isHeaderRow = firstRowIsHeader && rows.isEmpty();
                for (int i = 0; i < row.length; i++) {
                    if (row[i] == null || (!isHeaderRow && extraNullTokens.contains(row[i]))) {
                        row[i] = canonicalNullToken;
                    }
                }
                rows.add(row);
            }
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
     * The first row of {@code content}, read raw, or an <b>empty list</b> if the document holds no
     * row at all.
     *
     * <p>Empty rather than an exception because the callers disagree about what a missing header
     * means. On the labelled path it is fatal and the caller says so. On the plain path it is not
     * this method's business: an empty document is simply an empty document, and jackson's own
     * {@code CsvReadException} ("Empty header line: can not bind data") — raised by the read that
     * follows — describes it better than anything thrown from here, which would otherwise report a
     * labelled-CSV failure on a read that is not labelled.</p>
     */
    private List<String> readHeaderLabels(String content) throws IOException {
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
                // Applies schema-wide rather than per column: jackson's CsvGenerator recognises a
                // multi-valued property by the writeStartArray()/writeEndArray() calls its own bean
                // serialiser makes, regardless of the column's declared ColumnType, and always
                // consults this one schema-level separator to join the elements into the cell.
                .setArrayElementSeparator(configuration.getListDelimiter());
        if (dialect.getEscapeChar() == null) {
            // No escape character — RFC 4180's doubled quote, and the default. See CsvDialect's
            // javadoc for why this is null rather than the quote character.
            builder.disableEscapeChar();
        } else {
            builder.setEscapeChar(dialect.getEscapeChar());
        }
        List<String> nullTokens = configuration.getNullTokens();
        if (!nullTokens.isEmpty()) {
            // CsvSchema carries exactly one null-value token. The first (canonical) entry of
            // nullTokens goes here: on read, a cell exactly matching it is reported as VALUE_NULL by
            // the parser itself, before any type-specific (number/date/boolean) text parsing is
            // attempted; on write, it is the token an absent value is written back as. A
            // configuration naming further tokens is handled by normalizeExtraNullTokens instead,
            // because the schema has no second slot.
            builder.setNullValue(nullTokens.get(0));
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
     * Rejects a list element this mapper could write but could not read back. Two kinds cannot
     * round-trip, and both are rejected here, at the point the bad value is known, rather than
     * leaving the writer to emit a file its own reader refuses:
     *
     * <ul>
     *   <li>an element <b>containing the configured list delimiter</b>. Jackson's array handling has
     *       no escape for it (RFC 4180 quoting is a cell-level concern, already consumed by the time
     *       the cell is split on this delimiter), so a two-element list containing it would be
     *       silently written and read back as three; and</li>
     *   <li>an element <b>equal to the canonical null token</b> ({@code nullTokens.get(0)}) — most
     *       commonly the empty string under the default {@code nullTokens=[""]}. Written out, such an
     *       element is indistinguishable from one meaning "absent", and
     *       {@link #stripListElementsMatchingTheCanonicalNullToken} drops those on read. Refusing it
     *       here is what stops the writer silently shortening a list: the value would come back with
     *       one element fewer, which for a {@code (1..*)} attribute is a cardinality violation rather
     *       than a merely different string.</li>
     * </ul>
     *
     * <p>Only the canonical token is checked, because only it is stripped on read. An element equal to
     * a <em>later</em> null token round-trips losslessly — those tokens apply to whole cells only —
     * so refusing it would reject a value this mapper reads back perfectly.</p>
     *
     * <p>The second check is deliberately the exact counterpart of
     * {@link #stripListElementsMatchingTheCanonicalNullToken}: the writer refuses to emit precisely
     * the elements the reader would drop, so the reader never silently loses data this writer
     * produced. <b>The two must be changed together.</b> Note the asymmetry is intended — loud on
     * write, forgiving on read — which is the robustness principle the Rune serialisation spec §5.3
     * states, and no worse than the scalar case, where an empty-string value written under the default
     * configuration already comes back absent with no error at all.</p>
     *
     * <p>Only {@link RosettaModelObject} values are checked — a plain POJO (as
     * {@link #schemaInDeclarationOrder(Object)} already accommodates) has no {@code process} visitor
     * to walk.</p>
     *
     * @throws IllegalArgumentException naming the attribute and the offending value
     */
    private void rejectListElementsThatCannotRoundTrip(Object value) {
        if (!(value instanceof RosettaModelObject)) {
            return;
        }
        RosettaModelObject instance = (RosettaModelObject) value;
        List<String> nullTokens = configuration.getNullTokens();
        instance.process(RosettaPath.valueOf(instance.getType().getSimpleName()),
                new ListElementCollisionDetector(configuration.getListDelimiter(),
                        nullTokens.isEmpty() ? null : nullTokens.get(0)));
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

    /**
     * Walks a value's simple attributes looking for a multi-valued one holding an element that
     * cannot round-trip, throwing as soon as it finds one. Declines to recurse into a complex
     * attribute for the same reason {@link DeclarationOrderCollector} does: tabular CSV types have
     * none.
     */
    private static final class ListElementCollisionDetector implements Processor {
        private final String listDelimiter;
        private final String canonicalNullToken;

        /**
         * @param canonicalNullToken {@code nullTokens.get(0)}, or {@code null} when no null token is
         *                           configured and so no element text can mean "absent"
         */
        private ListElementCollisionDetector(String listDelimiter, String canonicalNullToken) {
            this.listDelimiter = listDelimiter;
            this.canonicalNullToken = canonicalNullToken;
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
                if (text.equals(canonicalNullToken)) {
                    throw new IllegalArgumentException(String.format(
                            "Cannot serialise attribute '%s' to CSV: element '%s' is the canonical null token "
                                    + "(nullTokens[0]), so it would be written as a list element indistinguishable "
                                    + "from an absent value and dropped when read back, silently shortening the "
                                    + "list. Either change the value or reconfigure nullTokens so it does not "
                                    + "collide.",
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
     * canonical null token — so {@code "EUR;"} reads as {@code [EUR]} and {@code "EUR;;USD"} as
     * {@code [EUR, USD]}.
     *
     * <p><b>Why an element that means "absent" is dropped rather than kept or rejected.</b> A cell
     * legitimately holding several elements can produce an empty one from a trailing or doubled
     * {@code listDelimiter}, and under the default {@code nullTokens=[""]} an empty element is
     * indistinguishable from the token meaning "absent". Rune has no way to represent that: {@code
     * empty} is the absence of a value, and for a multi-valued attribute it means the empty
     * <em>list</em>, never a hole within one. The generated immutable rejects a {@code null} element
     * outright ({@code ImmutableList.copyOf}), and the language layer never surfaces one either —
     * {@code MapperC.of(List)} marks it an error item and {@code getMulti()} filters those out.
     * Dropping it here is therefore the same rule the rest of the stack already applies, and the
     * direct analogue of the scalar case: a cell that means "absent" makes its attribute absent, so
     * an element that means "absent" makes its element absent. It matches the Rune serialisation
     * spec §5.2.1 ("when a null value is encountered it will be ignored") and the generated
     * {@code addTags(String)} adder, which silently drops a {@code null}.</p>
     *
     * <p><b>Only the canonical null token</b> — {@code nullTokens.get(0)}, the one
     * {@link #dialectSchema} stamps onto the schema — is stripped. Tokens beyond the first apply to
     * <b>whole cells only</b>: {@link #normalizeExtraNullTokens} substitutes them cell by cell and
     * never looks inside one, so an element equal to a later token is an ordinary string and reading
     * it back is lossless. Widening this to every token would destroy data that reads correctly: with
     * {@code nullTokens=["", "N/A"]} the cell {@code "EUR;N/A"} deserialises to {@code [EUR, N/A]}.</p>
     *
     * <p>A cell that splits into a <b>single</b> element is left untouched, whatever its text. That is
     * the whole-cell case, which belongs to the schema's own null-value handling — stripping it here
     * would turn a cell meaning "absent list" into an empty cell meaning the same thing by a longer
     * route, and for a non-empty canonical token it would change the bytes jackson sees.</p>
     *
     * <p>Skips entirely when there is nothing to strip — no configured null tokens, or no
     * multi-cardinality attribute on {@code valueType} — so a type with only scalar attributes pays
     * no extra parsing pass, and returns {@code content} unchanged (not a re-rendered copy of it)
     * when no cell actually needed stripping. Both guards run <b>before</b> the header row is read,
     * which is why the header is read here rather than passed in by the plain-path caller: an
     * argument would be evaluated first and the guards would no longer be able to prevent the work.</p>
     *
     * @param columnAttributeNames the attribute each column binds to, in column order, or
     *                             {@code null} to read them from the header row. The labelled path
     *                             supplies them because its header holds labels rather than
     *                             attribute names and it has already resolved the mapping; the plain
     *                             path passes {@code null}, its header being attribute names already
     * @return {@code content} with those elements removed, or {@code content} itself if none were
     */
    private String stripListElementsMatchingTheCanonicalNullToken(String content, Class<?> valueType,
            List<String> columnAttributeNames) throws IOException {
        List<String> nullTokens = configuration.getNullTokens();
        if (nullTokens.isEmpty()) {
            return content;
        }
        Set<String> multiValuedAttributes = multiValuedAttributeNames(valueType);
        if (multiValuedAttributes.isEmpty()) {
            return content;
        }
        String canonicalNullToken = nullTokens.get(0);
        List<String> columns = columnAttributeNames != null ? columnAttributeNames : readHeaderLabels(content);
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
                        // Restored to text for the same reason normalizeExtraNullTokens does it: the
                        // generator omits a null element from a column-less schema, shifting every
                        // later column one place left. Never on its own a reason to re-emit, so it
                        // does not set the flag.
                        row[i] = canonicalNullToken;
                        continue;
                    }
                    if (isHeaderRow || i >= columns.size() || !multiValuedAttributes.contains(columns.get(i))) {
                        continue;
                    }
                    String cell = withoutElementsMatching(row[i], canonicalNullToken);
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
     * {@code cell} with every list element equal to {@code canonicalNullToken} removed, or {@code cell}
     * itself if it holds a single element or none matched. A cell whose every element matched becomes
     * the empty string, which reads back as an absent list — the same value an empty cell gives.
     */
    private String withoutElementsMatching(String cell, String canonicalNullToken) {
        String listDelimiter = configuration.getListDelimiter();
        String[] elements = cell.split(Pattern.quote(listDelimiter), -1);
        if (elements.length < 2) {
            return cell;
        }
        List<String> kept = new ArrayList<>(elements.length);
        for (String element : elements) {
            if (!element.equals(canonicalNullToken)) {
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
