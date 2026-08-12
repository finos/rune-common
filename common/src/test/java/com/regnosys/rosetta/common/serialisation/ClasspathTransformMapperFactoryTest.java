package com.regnosys.rosetta.common.serialisation;

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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.regnosys.rosetta.common.serialisation.csv.config.CsvDialect;
import com.regnosys.rosetta.common.serialisation.csv.config.RosettaCSVConfiguration;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.annotations.RuneLabelProvider;
import com.rosetta.model.lib.functions.LabelProvider;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.transform.Enrich;
import com.rosetta.model.lib.transform.Ingest;
import com.rosetta.model.lib.transform.Projection;
import com.rosetta.model.lib.transform.SerializationFormat;
import csv.test.user.User;
import org.finos.rune.mapper.RuneJsonObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the end-to-end annotation-driven path: {@link TransformSerializationResolver} decides,
 * {@link ClasspathTransformMapperFactory} constructs.
 */
class ClasspathTransformMapperFactoryTest {

    private static final String XML_CONFIG = "serialisation/xml/xml-config/extension-schema-xml-config.json";
    private static final String CSV_CONFIG = "serialisation/csv/csv-config/semicolon-csv-config.json";
    private static final String CSV_LABELLED_CONFIG = "serialisation/csv/csv-config/labelled-semicolon-csv-config.json";

    private final ClasspathTransformMapperFactory factory = new ClasspathTransformMapperFactory();

    private Optional<ObjectMapper> inputMapper(Class<?> functionClass) {
        return TransformSerializationResolver.input(functionClass).map(s -> factory.create(s, functionClass));
    }

    private Optional<ObjectMapper> outputMapper(Class<?> functionClass) {
        return TransformSerializationResolver.output(functionClass).map(s -> factory.create(s, functionClass));
    }

    @Ingest(id = "extensionSchema", format = SerializationFormat.XML, configPath = XML_CONFIG)
    private static class XmlSchemaIngest {
    }

    @Ingest(format = SerializationFormat.XML)
    private static class BareXmlIngest {
    }

    @Ingest(format = SerializationFormat.JSON)
    private static class JsonIngest {
    }

    @Projection(format = SerializationFormat.RUNE_JSON)
    private static class RuneJsonProjection {
    }

    @Projection(format = SerializationFormat.CSV)
    private static class CsvProjection {
    }

    @Projection(format = SerializationFormat.CSV, configPath = CSV_CONFIG)
    private static class CsvProjectionWithConfig {
    }

    @Enrich
    private static class Enricher {
    }

    private static class NotAnnotated {
    }

    public static class TestLabelProvider implements LabelProvider {
        @Override
        public String getLabel(RosettaPath path) {
            return null;
        }
    }

    @Projection(format = SerializationFormat.CSV_LABELLED)
    @RuneLabelProvider(labelProvider = TestLabelProvider.class)
    private static class CsvLabelledProjection implements RosettaFunction {
    }

    @Projection(format = SerializationFormat.CSV_LABELLED)
    private static class CsvLabelledProjectionWithoutLabelProvider implements RosettaFunction {
    }

    // ---------------------------------------------------------------------------
    // Type-first resolution, with the guard keyed on the caller-supplied transform side
    // ---------------------------------------------------------------------------

    private static User buildUser() {
        return User.builder()
                .setFirstName("Alice")
                .setIdentifier("id-001")
                .setLastName("Smith")
                .setUsername("asmith")
                .build();
    }

    /** Header of the first line written for {@link #buildUser()} — proves which provider was used. */
    private static String header(ObjectMapper mapper) throws JsonProcessingException {
        String csv = mapper.writeValueAsString(buildUser());
        return csv.substring(0, csv.indexOf('\n'));
    }

    /** Strips jackson-csv's own quote characters, for asserting field content independent of its quoting heuristic. */
    private static String unquoted(String csvLine) {
        return csvLine.replace("\"", "");
    }

    /** Distinguishable from {@link FunctionLabelProvider} so a test can prove which one was picked. */
    public static class TypeLabelProvider implements LabelProvider {
        @Override
        public String getLabel(RosettaPath path) {
            return "type:" + path.buildPath();
        }
    }

    /** Distinguishable from {@link TypeLabelProvider} so a test can prove which one was picked. */
    public static class FunctionLabelProvider implements LabelProvider {
        @Override
        public String getLabel(RosettaPath path) {
            return "func:" + path.buildPath();
        }
    }

    /**
     * Stub pojo interface carrying its own type-rooted provider — the CSV input/output type. Only a
     * {@link RosettaModelObject} can carry one; these stubs are never instantiated (resolution only
     * reflects on the class), so they need not implement the model methods.
     */
    @RuneLabelProvider(labelProvider = TypeLabelProvider.class)
    private interface LabelledRootType extends RosettaModelObject {
    }

    /** The generated "…Impl" shape: no annotation of its own, found only via the supertype search. */
    private abstract static class LabelledRootTypeImpl implements LabelledRootType {
    }

    /** A root type with no provider anywhere in its hierarchy — the nested-labels-only case. */
    private abstract static class UnlabelledRootType implements RosettaModelObject {
    }

    /**
     * A projection whose function-rooted provider is valid (this serialization is its {@code @Projection}
     * side) — used to prove the guard does not strip a working projection, and that type-first still wins
     * when both a type and a function provider exist and are reachable.
     */
    @Projection(format = SerializationFormat.CSV_LABELLED)
    @RuneLabelProvider(labelProvider = FunctionLabelProvider.class)
    private static class CsvLabelledProjectionWithFunctionProvider implements RosettaFunction {
    }

    @Projection(format = SerializationFormat.CSV_LABELLED, configPath = CSV_LABELLED_CONFIG)
    @RuneLabelProvider(labelProvider = FunctionLabelProvider.class)
    private static class CsvLabelledProjectionWithConfig implements RosettaFunction {
    }

    /**
     * The plain {@code CSV} format pointed at a config declaring {@code headerStyle: LABEL} — the shape
     * that replaces {@code CSV_LABELLED}. The provider is resolved because the <em>config</em> asks for
     * labels, not because the format does.
     */
    @Projection(format = SerializationFormat.CSV, configPath = CSV_LABELLED_CONFIG)
    @RuneLabelProvider(labelProvider = FunctionLabelProvider.class)
    private static class CsvProjectionWithLabelConfig implements RosettaFunction {
    }

    /**
     * The same, as an ingest: its function-rooted provider is rooted at its <b>output</b>, so on the
     * declared input side there is nothing legitimate to label with.
     */
    @Ingest(format = SerializationFormat.CSV, configPath = CSV_LABELLED_CONFIG)
    @RuneLabelProvider(labelProvider = FunctionLabelProvider.class)
    private static class CsvIngestWithLabelConfig implements RosettaFunction {
    }

    /**
     * A labelled function whose {@code CSV} config leaves {@code headerStyle} at {@code ATTRIBUTE_NAME}. The
     * format does not decide, so this must <b>not</b> acquire labels from its function.
     */
    @Projection(format = SerializationFormat.CSV, configPath = CSV_CONFIG)
    @RuneLabelProvider(labelProvider = FunctionLabelProvider.class)
    private static class CsvProjectionWithNonLabelConfigAndProvider implements RosettaFunction {
    }

    /**
     * An ingest whose function-rooted provider is rooted at its <b>output</b> type, not the serialised
     * input — the case the guard exists to reject.
     */
    @Ingest(format = SerializationFormat.CSV_LABELLED)
    @RuneLabelProvider(labelProvider = FunctionLabelProvider.class)
    private static class CsvLabelledIngestWithFunctionProvider implements RosettaFunction {
    }

    /**
     * A labelled function carrying no transform annotation at all — the shape of a report, an
     * enrichment, and a model generated before transform annotations existed. Its provider is rooted at
     * its output exactly as a projection's is; nothing in its annotations says so.
     */
    @RuneLabelProvider(labelProvider = FunctionLabelProvider.class)
    private static class LabelledFunctionWithoutTransformAnnotation implements RosettaFunction {
    }

    /**
     * A CSV-to-CSV transform: both sides declare {@code CSV_LABELLED}, so
     * {@link TransformSerializationResolver#input} and {@link TransformSerializationResolver#output}
     * return <em>equal</em> values. Its provider is rooted at its output like any other function's.
     */
    @Ingest(format = SerializationFormat.CSV_LABELLED)
    @Projection(format = SerializationFormat.CSV_LABELLED)
    @RuneLabelProvider(labelProvider = FunctionLabelProvider.class)
    private static class CsvToCsvFunctionWithFunctionProvider implements RosettaFunction {
    }

    @Test
    void csvLabelledProjectionWithRootTypePrefersTheTypeProviderOverTheFunctionProvider() throws JsonProcessingException {
        TransformSerialization s = TransformSerializationResolver.output(CsvLabelledProjectionWithFunctionProvider.class).get();
        ObjectMapper mapper = factory.create(s, CsvLabelledProjectionWithFunctionProvider.class,
                TransformRoot.output(LabelledRootType.class));

        assertEquals("type:username,type:identifier,type:firstName,type:lastName", header(mapper));
    }

    @Test
    void csvLabelledProjectionWithoutRootTypeStillUsesTheFunctionProvider() throws JsonProcessingException {
        // Regression guard for today's working path: no root type supplied, so resolution falls back to
        // the function's own provider.
        TransformSerialization s = TransformSerializationResolver.output(CsvLabelledProjectionWithFunctionProvider.class).get();
        ObjectMapper mapper = factory.create(s, CsvLabelledProjectionWithFunctionProvider.class);

        assertEquals("func:username,func:identifier,func:firstName,func:lastName", header(mapper));
    }

    @Test
    void csvLabelledProjectionWithUnlabelledRootTypeFallsBackToTheFunctionProvider() throws JsonProcessingException {
        // The nested-labels-only shape: the root type carries no type-rooted provider of its own, so the
        // guard must not strip the function's provider from an otherwise-working projection.
        TransformSerialization s = TransformSerializationResolver.output(CsvLabelledProjectionWithFunctionProvider.class).get();
        ObjectMapper mapper = factory.create(s, CsvLabelledProjectionWithFunctionProvider.class,
                TransformRoot.output(UnlabelledRootType.class));

        assertEquals("func:username,func:identifier,func:firstName,func:lastName", header(mapper));
    }

    @Test
    void csvLabelledIngestOnTheDeclaredInputSideGetsNoProviderAndDegradesToPlainCsv() throws JsonProcessingException {
        // A function-rooted provider is rooted at the function's OUTPUT, so an ingest reading its INPUT
        // may never use it. The caller declared the input side, so the guard rejects it, and there is no
        // root type to try instead.
        TransformSerialization s = TransformSerializationResolver.input(CsvLabelledIngestWithFunctionProvider.class).get();
        ObjectMapper mapper = factory.create(s, CsvLabelledIngestWithFunctionProvider.class, TransformRoot.input());

        assertEquals("username,identifier,firstName,lastName", header(mapper));
    }

    @Test
    void csvLabelledIngestWithNoDeclaredSideKeepsTodaysFunctionProvider() throws JsonProcessingException {
        // The guard is the caller's declared side and nothing else. A caller that has not been updated
        // supplies no root, so it keeps exactly the behaviour it had before root context existed.
        TransformSerialization s = TransformSerializationResolver.input(CsvLabelledIngestWithFunctionProvider.class).get();
        ObjectMapper mapper = factory.create(s, CsvLabelledIngestWithFunctionProvider.class);

        assertEquals("func:username,func:identifier,func:firstName,func:lastName", header(mapper));
    }

    @Test
    void labelledFunctionWithoutATransformAnnotationKeepsItsLabelsWhenNoSideIsDeclared() throws JsonProcessingException {
        // Reports, enrichments and pre-annotation models all carry a label provider and no @Projection.
        // A guard that read the annotations would strip the labels off every one of them.
        ObjectMapper mapper = factory.create(new TransformSerialization(SerializationFormat.CSV_LABELLED, null),
                LabelledFunctionWithoutTransformAnnotation.class);

        assertEquals("func:username,func:identifier,func:firstName,func:lastName", header(mapper));
    }

    @Test
    void labelledFunctionWithoutATransformAnnotationKeepsItsLabelsOnTheDeclaredOutputSide() throws JsonProcessingException {
        // …and declaring the side it really is must not strip them either: the provider is rooted at the
        // function's output, which is precisely what the caller is serializing.
        ObjectMapper mapper = factory.create(new TransformSerialization(SerializationFormat.CSV_LABELLED, null),
                LabelledFunctionWithoutTransformAnnotation.class, TransformRoot.output());

        assertEquals("func:username,func:identifier,func:firstName,func:lastName", header(mapper));
    }

    @Test
    void csvToCsvTransformKeepsItsFunctionProviderOnTheDeclaredOutputSide() throws JsonProcessingException {
        TransformSerialization s = TransformSerializationResolver.output(CsvToCsvFunctionWithFunctionProvider.class).get();
        ObjectMapper mapper = factory.create(s, CsvToCsvFunctionWithFunctionProvider.class, TransformRoot.output());

        assertEquals("func:username,func:identifier,func:firstName,func:lastName", header(mapper));
    }

    @Test
    void csvToCsvTransformRefusesItsFunctionProviderOnTheDeclaredInputSide() throws JsonProcessingException {
        // Read this test together with the one above. Both sides of this function declare CSV_LABELLED,
        // so the two TransformSerialization values are equal and no comparison of them can tell the sides
        // apart. Only the caller's declared side does, and the input side must never receive a provider
        // rooted at the output.
        TransformSerialization input = TransformSerializationResolver.input(CsvToCsvFunctionWithFunctionProvider.class).get();
        TransformSerialization output = TransformSerializationResolver.output(CsvToCsvFunctionWithFunctionProvider.class).get();
        assertEquals(output, input, "this test is only meaningful while the two sides are indistinguishable");

        ObjectMapper mapper = factory.create(input, CsvToCsvFunctionWithFunctionProvider.class, TransformRoot.input());

        assertEquals("username,identifier,firstName,lastName", header(mapper));
    }

    @Test
    void csvLabelledIngestWithLabelledInputTypeUsesTheTypeProvider() throws JsonProcessingException {
        // The CSV-import case: an ingest's function-rooted provider is wrongly
        // rooted at its output, but the caller now supplies the CSV input type as rootType, and type-first
        // resolution picks that up regardless of the (rejected) function provider.
        TransformSerialization s = TransformSerializationResolver.input(CsvLabelledIngestWithFunctionProvider.class).get();
        ObjectMapper mapper = factory.create(s, CsvLabelledIngestWithFunctionProvider.class,
                TransformRoot.input(LabelledRootType.class));

        assertEquals("type:username,type:identifier,type:firstName,type:lastName", header(mapper));
    }

    @Test
    void csvLabelledResolvesTheTypeProviderThroughAnImplClass() throws JsonProcessingException {
        // The supertype search applies here too: passing the "…Impl" shape as rootType, rather than the
        // pojo interface itself, must still find the interface's annotation.
        ObjectMapper mapper = factory.create(new TransformSerialization(SerializationFormat.CSV_LABELLED, null),
                null, TransformRoot.input(LabelledRootTypeImpl.class));

        assertEquals("type:username,type:identifier,type:firstName,type:lastName", header(mapper));
    }

    // ---------------------------------------------------------------------------
    // The WARN that explains a degrade — the only signal a caller gets that labels went missing
    // ---------------------------------------------------------------------------

    /** The factory's WARNs emitted while {@code work} runs. */
    private static List<String> warningsWhile(Runnable work) {
        Logger logger = (Logger) LoggerFactory.getLogger(ClasspathTransformMapperFactory.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            work.run();
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
        return appender.list.stream()
                .filter(event -> event.getLevel() == Level.WARN)
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
    }

    private static String onlyWarning(Runnable work) {
        List<String> warnings = warningsWhile(work);
        assertEquals(1, warnings.size(), "expected exactly one WARN, got " + warnings);
        return warnings.get(0);
    }

    @Test
    void suppressedFunctionProviderWarnsNamingTheFunctionAndWhy() {
        TransformSerialization s = TransformSerializationResolver.input(CsvLabelledIngestWithFunctionProvider.class).get();

        String warning = onlyWarning(
                () -> factory.create(s, CsvLabelledIngestWithFunctionProvider.class, TransformRoot.input()));

        // The whole mitigation: the first ingest to lose its (wrongly-rooted) labels must be able to
        // read why out of the log, without reading this class.
        assertTrue(warning.contains(CsvLabelledIngestWithFunctionProvider.class.getName()),
                "the suppressed function must be named: " + warning);
        assertTrue(warning.contains("rooted at its own output"), warning);
        assertTrue(warning.contains("falling back to unlabelled CSV"), warning);
    }

    @Test
    void unlabelledRootTypeWarnsNamingTheRootType() {
        TransformSerialization s = new TransformSerialization(SerializationFormat.CSV_LABELLED, null);

        String warning = onlyWarning(() -> factory.create(s, null, TransformRoot.output(UnlabelledRootType.class)));

        assertTrue(warning.contains(UnlabelledRootType.class.getName()),
                "the root type that came up empty must be named: " + warning);
    }

    @Test
    void noProviderAnywhereWarnsWithoutClaimingOneWasSuppressed() {
        TransformSerialization s = TransformSerializationResolver.output(CsvLabelledProjectionWithoutLabelProvider.class).get();

        String warning = onlyWarning(() -> factory.create(s, CsvLabelledProjectionWithoutLabelProvider.class));

        assertTrue(warning.contains("no @RuneLabelProvider could be resolved"), warning);
        assertFalse(warning.contains("rooted at its own output"),
                "nothing was suppressed here — there was no provider to suppress: " + warning);
    }

    @Test
    void aResolvedProviderWarnsAboutNothing() {
        TransformSerialization s = TransformSerializationResolver.output(CsvLabelledProjectionWithFunctionProvider.class).get();

        assertEquals(Collections.emptyList(),
                warningsWhile(() -> factory.create(s, CsvLabelledProjectionWithFunctionProvider.class,
                        TransformRoot.output(LabelledRootType.class))));
    }

    // ---------------------------------------------------------------------------
    // The deprecated overloads still do what they did
    // ---------------------------------------------------------------------------

    @Test
    @SuppressWarnings("deprecation")
    void deprecatedCsvLabelledMapperOverloadKeepsItsPreTypeFirstBehaviour() throws JsonProcessingException {
        // It is kept for subclasses that call it, so it must still resolve from the function alone — no
        // root type, no guard — even for an ingest, whose provider the create path now rejects.
        ObjectMapper mapper = factory.csvLabelledMapper(CsvLabelledIngestWithFunctionProvider.class);

        assertEquals("func:username,func:identifier,func:firstName,func:lastName", header(mapper));
    }

    /** Distinguishable again, so a test can prove a subclass's override was the one consulted. */
    public static class SubclassLabelProvider implements LabelProvider {
        @Override
        public String getLabel(RosettaPath path) {
            return "sub:" + path.buildPath();
        }
    }

    /** The pre-existing extension shape: a subclass that overrode the single-argument hook. */
    private static class OverridingResolutionFactory extends ClasspathTransformMapperFactory {
        @Override
        @Deprecated
        protected LabelProvider resolveLabelProvider(Class<?> functionClass) {
            return new SubclassLabelProvider();
        }
    }

    @Test
    void anExistingSubclassOverrideStillDecidesTheFunctionRootedBranch() throws JsonProcessingException {
        // A hook kept but never called is worse than one removed: it compiles, runs and silently stops
        // deciding anything. On the branch it was written for, it still decides.
        TransformSerialization s = TransformSerializationResolver.output(CsvLabelledProjectionWithFunctionProvider.class).get();
        ObjectMapper mapper = new OverridingResolutionFactory()
                .create(s, CsvLabelledProjectionWithFunctionProvider.class);

        assertEquals("sub:username,sub:identifier,sub:firstName,sub:lastName", header(mapper));
    }

    /** The other pre-existing extension shape: a subclass that overrode the single-argument mapper hook. */
    private static class OverridingCsvLabelledMapperFactory extends ClasspathTransformMapperFactory {
        @Override
        @Deprecated
        protected ObjectMapper csvLabelledMapper(Class<?> functionClass) {
            return RosettaObjectMapperCreator.forCSV(new SubclassLabelProvider()).create();
        }
    }

    @Test
    void anExistingSubclassOverrideOfCsvLabelledMapperStillDecidesWhenNoRootIsSupplied() throws JsonProcessingException {
        // Same rule as for resolveLabelProvider(Class). A null root is exactly this overload's
        // pre-root-context case, so it still decides that case.
        TransformSerialization s = TransformSerializationResolver.output(CsvLabelledProjectionWithFunctionProvider.class).get();
        ObjectMapper mapper = new OverridingCsvLabelledMapperFactory()
                .create(s, CsvLabelledProjectionWithFunctionProvider.class);

        assertEquals("sub:username,sub:identifier,sub:firstName,sub:lastName", header(mapper));
    }

    @Test
    void anExistingSubclassOverrideOfCsvLabelledMapperDoesNotOutrankASuppliedRoot() throws JsonProcessingException {
        // …and it no longer answers for a caller that supplied a root: that caller knows something the
        // override predates.
        TransformSerialization s = TransformSerializationResolver.output(CsvLabelledProjectionWithFunctionProvider.class).get();
        ObjectMapper mapper = new OverridingCsvLabelledMapperFactory()
                .create(s, CsvLabelledProjectionWithFunctionProvider.class, TransformRoot.output(LabelledRootType.class));

        assertEquals("type:username,type:identifier,type:firstName,type:lastName", header(mapper));
    }

    @Test
    void anExistingSubclassOverrideDoesNotOutrankTheTypeProvider() throws JsonProcessingException {
        // …but it no longer answers for the whole of resolution: type-first still wins.
        TransformSerialization s = TransformSerializationResolver.output(CsvLabelledProjectionWithFunctionProvider.class).get();
        ObjectMapper mapper = new OverridingResolutionFactory()
                .create(s, CsvLabelledProjectionWithFunctionProvider.class, TransformRoot.output(LabelledRootType.class));

        assertEquals("type:username,type:identifier,type:firstName,type:lastName", header(mapper));
    }

    @Test
    void buildsXmlMapperFromSchemaConfigPath() {
        assertInstanceOf(XmlMapper.class, inputMapper(XmlSchemaIngest.class).get());
    }

    @Test
    void buildsXmlMapperForBareFormatWithoutConfigPath() {
        assertInstanceOf(XmlMapper.class, inputMapper(BareXmlIngest.class).get());
    }

    @Test
    void buildsJsonMapperForJsonIngest() {
        assertTrue(inputMapper(JsonIngest.class).isPresent());
    }

    @Test
    void buildsRuneJsonMapperForRuneJsonProjection() {
        assertInstanceOf(RuneJsonObjectMapper.class, outputMapper(RuneJsonProjection.class).get());
    }

    @Test
    void buildsCsvMapperForCsvProjection() {
        assertTrue(outputMapper(CsvProjection.class).isPresent());
    }

    @Test
    void buildsCsvMapperForCsvLabelledProjectionUsingAnnotatedLabelProvider() {
        assertTrue(outputMapper(CsvLabelledProjection.class).isPresent());
    }

    // ---------------------------------------------------------------------------
    // Resolving the CSV configuration from the classpath, mirroring the XML branch
    // ---------------------------------------------------------------------------

    @Test
    void csvMapperWithNoConfigPathBehavesExactlyAsToday() throws JsonProcessingException {
        ObjectMapper mapper = outputMapper(CsvProjection.class).get();
        assertEquals("username,identifier,firstName,lastName", header(mapper));
    }

    @Test
    void csvMapperWithConfigPathUsesTheConfiguredDialect() throws JsonProcessingException {
        ObjectMapper mapper = outputMapper(CsvProjectionWithConfig.class).get();
        assertEquals("username;identifier;firstName;lastName", header(mapper));
    }

    /**
     * The plain {@code CSV} format serves labelled CSV when — and only when — its configuration asks for it. Both halves of the config take effect, the semicolon dialect
     * and the LABEL header style.
     * <p>
     * jackson-csv's own quoting heuristic quotes any character below max(separator, quoteChar) + 1, so
     * ':' (58) gets quoted once the separator is ';' (59) even though ':' is not the separator — stripped
     * here since the config taking effect, not jackson's quoting choice, is what's under test.
     */
    @Test
    void csvMapperWithALabelConfigIsLabelled() throws JsonProcessingException {
        TransformSerialization s = TransformSerializationResolver.output(CsvProjectionWithLabelConfig.class).get();
        ObjectMapper mapper = factory.create(s, CsvProjectionWithLabelConfig.class, TransformRoot.output());

        assertEquals("func:username;func:identifier;func:firstName;func:lastName", unquoted(header(mapper)));
    }

    /**
     * The complement: the same format on a function that <em>does</em> carry {@code @RuneLabelProvider},
     * whose config does not ask for labels, must stay plain. A plain CSV transform must not acquire labels
     * merely because its function has a provider.
     */
    @Test
    void csvMapperWithANonLabelConfigStaysPlainEvenWithAProviderAvailable() throws JsonProcessingException {
        TransformSerialization s =
                TransformSerializationResolver.output(CsvProjectionWithNonLabelConfigAndProvider.class).get();
        ObjectMapper mapper =
                factory.create(s, CsvProjectionWithNonLabelConfigAndProvider.class, TransformRoot.output());

        assertEquals("username;identifier;firstName;lastName", header(mapper),
                "the config asked for ATTRIBUTE_NAME headers, so the function's provider must go unused");
    }

    /**
     * A {@code CSV} config asking for labels is honoured whether or not the caller supplies a root — which
     * is the property that makes this the migration target for {@code CSV_LABELLED}, whose config is
     * dropped on the root-less path. With no root nothing is suppressed, so the function's provider stands.
     */
    @Test
    void csvMapperWithALabelConfigIsHonouredWithNoRootAtAll() throws JsonProcessingException {
        TransformSerialization s = TransformSerializationResolver.output(CsvProjectionWithLabelConfig.class).get();

        ObjectMapper mapper = factory.create(s, CsvProjectionWithLabelConfig.class);

        assertEquals("func:username;func:identifier;func:firstName;func:lastName", unquoted(header(mapper)));
        assertEquals(Collections.emptyList(),
                warningsWhile(() -> factory.create(s, CsvProjectionWithLabelConfig.class)),
                "nothing was dropped and nothing degraded, so there is nothing to report");
    }

    /** A labelled ingest reads its labels from the root type — the only provider correct on that side. */
    @Test
    void csvMapperWithALabelConfigOnTheInputSideUsesTheRootTypeProvider() throws JsonProcessingException {
        TransformSerialization s = TransformSerializationResolver.input(CsvIngestWithLabelConfig.class).get();

        ObjectMapper mapper = factory.create(s, CsvIngestWithLabelConfig.class,
                TransformRoot.input(LabelledRootType.class));

        assertEquals("type:username;type:identifier;type:firstName;type:lastName", unquoted(header(mapper)));
    }

    /**
     * A config declaring {@code headerStyle=LABEL} for which no provider can be found <b>fails</b>, where
     * {@code CSV_LABELLED} degrades to plain CSV. Proceeding would mean treating LABEL as ATTRIBUTE_NAME,
     * discarding the one setting the configuration exists to state.
     * <p>
     * The message has to carry both ends of the diagnosis: which config asked, and why nothing could
     * answer. Here the function's provider exists but is rooted at its own output, and this is the
     * declared input side.
     */
    @Test
    void csvMapperWithALabelConfigAndNoResolvableProviderFails() {
        TransformSerialization s = TransformSerializationResolver.input(CsvIngestWithLabelConfig.class).get();

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> factory.create(s, CsvIngestWithLabelConfig.class, TransformRoot.input()));

        assertTrue(e.getMessage().contains(CSV_LABELLED_CONFIG), "the config that asked must be named: " + e.getMessage());
        assertTrue(e.getMessage().contains(CsvIngestWithLabelConfig.class.getName()),
                "the provider that could not be used must be named: " + e.getMessage());
        assertTrue(e.getMessage().contains("rooted at its own output"), e.getMessage());
        assertTrue(e.getMessage().contains("headerStyle=ATTRIBUTE_NAME"),
                "the way out via the config must be stated: " + e.getMessage());
    }

    /**
     * {@code CSV_LABELLED} reads no configuration, with a root or without one. Asserted in both directions:
     * this and the test below assert the comma where {@link #csvMapperWithALabelConfigIsLabelled} asserts the
     * semicolon the same config produces through the {@code CSV} format.
     */
    @Test
    void csvLabelledWithConfigPathDropsTheConfigWithNoRoot() throws JsonProcessingException {
        TransformSerialization s = TransformSerializationResolver.output(CsvLabelledProjectionWithConfig.class).get();

        ObjectMapper mapper = factory.create(s, CsvLabelledProjectionWithConfig.class);

        assertEquals("func:username,func:identifier,func:firstName,func:lastName", unquoted(header(mapper)),
                "the declared semicolon config is not applied to a CSV_LABELLED transform");
    }

    @Test
    void csvLabelledWithConfigPathDropsTheConfigWithARootToo() throws JsonProcessingException {
        // Supplying a root does not make the config apply either: this format reads none at all.
        TransformSerialization s = TransformSerializationResolver.output(CsvLabelledProjectionWithConfig.class).get();

        ObjectMapper mapper = factory.create(s, CsvLabelledProjectionWithConfig.class, TransformRoot.output());

        assertEquals("func:username,func:identifier,func:firstName,func:lastName", unquoted(header(mapper)));
    }

    @Test
    void csvLabelledDroppingADeclaredConfigPathSaysSo() {
        // A declared configPath is a configuration someone supplied. Dropped in silence, the transform
        // writes a well-formed comma-delimited file and nothing indicates its own config was never read.
        TransformSerialization s = TransformSerializationResolver.output(CsvLabelledProjectionWithConfig.class).get();

        String warning = onlyWarning(() -> factory.create(s, CsvLabelledProjectionWithConfig.class));

        assertTrue(warning.contains(CSV_LABELLED_CONFIG), "the dropped config path must be named: " + warning);
        assertTrue(warning.contains(CsvLabelledProjectionWithConfig.class.getName()), warning);
        assertTrue(warning.contains("format = CSV"), "the migration must be stated: " + warning);
        assertTrue(warning.contains("headerStyle"), "the migration must be stated: " + warning);
    }

    @Test
    void csvLabelledDroppingADeclaredConfigPathSaysSoWithARootToo() {
        // The WARN is no longer conditional on the root: the drop no longer is either.
        TransformSerialization s = TransformSerializationResolver.output(CsvLabelledProjectionWithConfig.class).get();

        String warning = onlyWarning(
                () -> factory.create(s, CsvLabelledProjectionWithConfig.class, TransformRoot.output()));

        assertTrue(warning.contains(CSV_LABELLED_CONFIG), warning);
    }

    @Test
    void csvLabelledWithNoConfigPathAndNoRootWarnsAboutNothing() {
        // The complement: nothing was supplied, so there is nothing to report. A warning here would fire
        // on every pre-config CSV_LABELLED transform in existence.
        TransformSerialization s = TransformSerializationResolver.output(CsvLabelledProjectionWithFunctionProvider.class).get();

        assertEquals(Collections.emptyList(),
                warningsWhile(() -> factory.create(s, CsvLabelledProjectionWithFunctionProvider.class)));
    }

    @Test
    void missingCsvConfigResourceIsReported() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> factory.create(new TransformSerialization(SerializationFormat.CSV, "does/not/exist.json"),
                        ClasspathTransformMapperFactoryTest.class));
        assertTrue(e.getMessage().contains("does/not/exist.json"));
    }

    /**
     * The counterpart of {@link #missingCsvConfigResourceIsReported}: a missing resource cannot be
     * reported for {@code CSV_LABELLED} because the format never looks one up. An unresolvable path is
     * simply part of the configuration it drops — which the WARN reports.
     */
    @Test
    void csvLabelledDoesNotEvenLookUpItsConfigResource() throws JsonProcessingException {
        TransformSerialization s = new TransformSerialization(SerializationFormat.CSV_LABELLED, "does/not/exist.json");

        ObjectMapper mapper = factory.create(s, CsvLabelledProjectionWithFunctionProvider.class, TransformRoot.output());

        assertEquals("func:username,func:identifier,func:firstName,func:lastName", header(mapper));
    }

    @Test
    void subclassOverridingOpenCsvConfigIsConsulted() throws JsonProcessingException {
        // A runtime keeping its CSV configuration somewhere other than the classpath overrides only
        // this hook, exactly as openXmlConfig already allows.
        ClasspathTransformMapperFactory overriding = new ClasspathTransformMapperFactory() {
            @Override
            protected InputStream openCsvConfig(String configPath, Class<?> functionClass) throws IOException {
                return new ByteArrayInputStream(
                        "{\"dialect\":{\"columnDelimiter\":\"|\"}}".getBytes(StandardCharsets.UTF_8));
            }
        };
        ObjectMapper mapper = overriding.create(
                new TransformSerialization(SerializationFormat.CSV, "irrelevant-on-this-override.json"),
                CsvProjection.class);

        // '|' is ascii 124, so jackson-csv's quoting heuristic (see the comment on the test above)
        // quotes every lowercase letter too; stripped for the same reason.
        assertEquals("username|identifier|firstName|lastName", unquoted(header(mapper)));
    }

    /**
     * A consequence of the configuration rather than the format deciding: since the header style is
     * configuration, a deployment that overrides where the configuration comes from can make a transform
     * labelled — here one whose classpath config asks for {@code ATTRIBUTE_NAME} headers. It still needs a
     * provider to resolve, by the ordinary rules.
     */
    @Test
    void aDeploymentSuppliedConfigurationCanMakeAPlainCsvTransformLabelled() throws JsonProcessingException {
        ClasspathTransformMapperFactory overriding = new ClasspathTransformMapperFactory() {
            @Override
            protected InputStream openCsvConfig(String configPath, Class<?> functionClass) {
                return new ByteArrayInputStream(
                        "{\"headerStyle\":\"LABEL\"}".getBytes(StandardCharsets.UTF_8));
            }
        };
        TransformSerialization s =
                TransformSerializationResolver.output(CsvProjectionWithNonLabelConfigAndProvider.class).get();

        ObjectMapper mapper =
                overriding.create(s, CsvProjectionWithNonLabelConfigAndProvider.class, TransformRoot.output());

        assertEquals("func:username,func:identifier,func:firstName,func:lastName", header(mapper),
                "the override's LABEL header style must outrank the classpath config's ATTRIBUTE_NAME");
    }

    /**
     * The deprecated no-argument {@code csvMapper()} is the released extension point for a transform that
     * declares no config path, so it must still decide that case. A subclass that overrode it before the
     * config path existed would otherwise compile, run and be silently ignored — the same treatment
     * {@code csvLabelledMapper(Class)} gets.
     */
    @Test
    @SuppressWarnings("deprecation")
    void subclassOverridingTheDeprecatedCsvMapperStillDecidesTheNoConfigPathCase() throws JsonProcessingException {
        ClasspathTransformMapperFactory overriding = new ClasspathTransformMapperFactory() {
            @Override
            protected ObjectMapper csvMapper() {
                return RosettaObjectMapperCreator.forCSV(
                        RosettaCSVConfiguration.builder()
                                .setDialect(CsvDialect.builder().setColumnDelimiter('|').build())
                                .build()).create();
            }
        };

        ObjectMapper mapper = overriding.create(
                new TransformSerialization(SerializationFormat.CSV, null), CsvProjection.class);

        assertEquals("username|identifier|firstName|lastName", unquoted(header(mapper)),
                "a transform declaring no configPath must still be served by the overridden csvMapper()");
    }

    /**
     * The complement, and what stops the delegation above from becoming a way to ignore a declared
     * configuration: a declared {@code configPath} outranks the deprecated overload, so the override is
     * not consulted and the config decides.
     */
    @Test
    @SuppressWarnings("deprecation")
    void aDeclaredConfigPathOutranksTheDeprecatedCsvMapperOverride() throws JsonProcessingException {
        ClasspathTransformMapperFactory overriding = new ClasspathTransformMapperFactory() {
            @Override
            protected ObjectMapper csvMapper() {
                throw new AssertionError("csvMapper() must not be consulted when a configPath is declared");
            }
        };

        ObjectMapper mapper = overriding.create(
                new TransformSerialization(SerializationFormat.CSV, CSV_CONFIG), CsvProjectionWithConfig.class);

        assertEquals("username;identifier;firstName;lastName", header(mapper));
    }

    // ---------------------------------------------------------------------------
    // Supplying the CSV configuration at deployment time
    // ---------------------------------------------------------------------------

    /** A deployment that keeps its CSV configuration outside the model artifact. */
    private static ClasspathTransformMapperFactory deploymentSupplying(String configJson) {
        return new ClasspathTransformMapperFactory() {
            @Override
            protected InputStream openCsvConfig(String configPath, Class<?> functionClass) {
                return new ByteArrayInputStream(configJson.getBytes(StandardCharsets.UTF_8));
            }
        };
    }

    /**
     * A client whose files are semicolon-delimited, served without rebuilding the model. Asserted as a round
     * trip: a header assertion alone would show the writer honouring the configuration while leaving the
     * reader untested.
     */
    @Test
    void aDeploymentSuppliedConfigurationRoundTripsSemicolonDelimitedCsv() throws IOException {
        // listDelimiter must move off its ';' default too, or the configuration is rejected for
        // collision with the column delimiter — which is the validation doing its job.
        ObjectMapper mapper = deploymentSupplying(
                "{\"dialect\":{\"columnDelimiter\":\";\"},\"listDelimiter\":\"|\"}")
                .create(new TransformSerialization(SerializationFormat.CSV, CSV_CONFIG), CsvProjectionWithConfig.class);

        String csv = mapper.writeValueAsString(buildUser());
        assertEquals("username;identifier;firstName;lastName", header(mapper));

        User roundTripped = mapper.readValue(csv, User.class);
        assertEquals(buildUser(), roundTripped, "the same configuration must serve the read side too");
    }

    /**
     * {@code hasHeader: false} reaches the mapper by the same deployment route. It is the setting most likely
     * to come from a deployment rather than a model: whether a production feed carries a header row is a
     * property of the feed, not of the type being ingested.
     */
    @Test
    void aDeploymentSuppliedConfigurationCanTurnOffTheHeaderRow() throws IOException {
        ObjectMapper mapper = deploymentSupplying("{\"hasHeader\":false}")
                .create(new TransformSerialization(SerializationFormat.CSV, CSV_CONFIG), CsvProjectionWithConfig.class);

        String csv = mapper.writeValueAsString(buildUser());
        assertEquals("asmith,id-001,Alice,Smith", header(mapper),
                "the first line must be data, not column names");

        assertEquals(buildUser(), mapper.readValue(csv, User.class),
                "the same configuration must serve the read side too");
    }

    /**
     * Precedence, asserted in both directions by one override that answers for its own path and delegates
     * the rest — the workspace-first shape {@code openXmlConfig} is already overridden with in
     * rosetta-products. Where the override answers it wins; where it delegates, the model's classpath
     * config stands.
     */
    @Test
    void aDeploymentOverrideWinsWhereItAnswersAndTheClasspathConfigStandsWhereItDelegates()
            throws JsonProcessingException {
        String deploymentOnlyPath = "deployment/pipe-csv-config.json";
        ClasspathTransformMapperFactory workspaceFirst = new ClasspathTransformMapperFactory() {
            @Override
            protected InputStream openCsvConfig(String configPath, Class<?> functionClass) throws IOException {
                if (deploymentOnlyPath.equals(configPath)) {
                    return new ByteArrayInputStream(
                            "{\"dialect\":{\"columnDelimiter\":\"|\"}}".getBytes(StandardCharsets.UTF_8));
                }
                return super.openCsvConfig(configPath, functionClass);
            }
        };

        ObjectMapper overridden = workspaceFirst.create(
                new TransformSerialization(SerializationFormat.CSV, deploymentOnlyPath), CsvProjection.class);
        // '|' is ascii 124, so jackson-csv's quoting heuristic (see the comment further up) quotes the
        // lowercase letters too; stripped, since the dialect is what is under test.
        assertEquals("username|identifier|firstName|lastName", unquoted(header(overridden)));

        ObjectMapper delegated = workspaceFirst.create(
                new TransformSerialization(SerializationFormat.CSV, CSV_CONFIG), CsvProjectionWithConfig.class);
        assertEquals("username;identifier;firstName;lastName", header(delegated),
                "a path the override declines must fall through to the model's classpath config");
    }

    /**
     * The accepted limitation: the hook is keyed on the config path, so a transform declaring none never reaches it and takes
     * {@code RosettaCSVConfiguration.EMPTY}. A deployment chooses the content behind a declared path; it
     * cannot introduce a configuration where the model asked for none.
     * <p>
     * The override here would return a pipe dialect if it were ever called. The comma proves it was not.
     */
    @Test
    void aDeploymentCannotSupplyAConfigurationForATransformThatDeclaresNoConfigPath()
            throws JsonProcessingException {
        ObjectMapper mapper = deploymentSupplying("{\"dialect\":{\"columnDelimiter\":\"|\"}}")
                .create(new TransformSerialization(SerializationFormat.CSV, null), CsvProjection.class);

        assertEquals("username,identifier,firstName,lastName", header(mapper));
    }

    @Test
    void csvLabelledWithoutRuneLabelProviderFallsBackToPlainCsv() {
        // No @RuneLabelProvider (e.g. a non-generated function): degrade to plain CSV rather than fail.
        assertTrue(outputMapper(CsvLabelledProjectionWithoutLabelProvider.class).isPresent());
    }

    @Test
    void csvLabelledFromFormatAloneFallsBackToPlainCsv() {
        // Built from the format alone (no function class -> no label provider): plain CSV, no exception.
        ObjectMapper mapper = factory.create(new TransformSerialization(SerializationFormat.CSV_LABELLED, null), null);
        assertNotNull(mapper);
    }

    @Test
    void enrichTransformHasNoObjectMapper() {
        assertFalse(inputMapper(Enricher.class).isPresent());
        assertFalse(outputMapper(Enricher.class).isPresent());
    }

    @Test
    void unannotatedClassHasNoObjectMapper() {
        assertFalse(inputMapper(NotAnnotated.class).isPresent());
        assertFalse(outputMapper(NotAnnotated.class).isPresent());
    }

    @Test
    void inputAndOutputResolveOnlyTheirOwnSide() {
        // an @Ingest class has an input mapper but no output mapper, and vice-versa
        assertTrue(inputMapper(JsonIngest.class).isPresent());
        assertFalse(outputMapper(JsonIngest.class).isPresent());
        assertTrue(outputMapper(CsvProjection.class).isPresent());
        assertFalse(inputMapper(CsvProjection.class).isPresent());
    }

    @Test
    void missingXmlConfigResourceIsReported() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> factory.create(new TransformSerialization(SerializationFormat.XML, "does/not/exist.json"),
                        ClasspathTransformMapperFactoryTest.class));
        assertTrue(e.getMessage().contains("does/not/exist.json"));
    }

    @Test
    void functionLessRequestsResolveAgainstTheOverriddenDefaultClassLoader() {
        // A runtime owning the model classloader overrides defaultClassLoader() so that requests
        // without a resolvable function class (e.g. a legacy pipeline definition) still resolve
        // the XML config against the model rather than the application classpath.
        ClassLoader modelClassLoader = ClasspathTransformMapperFactoryTest.class.getClassLoader();
        ClasspathTransformMapperFactory modelOwned = new ClasspathTransformMapperFactory() {
            @Override
            protected ClassLoader defaultClassLoader() {
                return modelClassLoader;
            }
        };
        ObjectMapper mapper = modelOwned.create(new TransformSerialization(SerializationFormat.XML, XML_CONFIG), null);
        assertInstanceOf(XmlMapper.class, mapper);
    }
}
