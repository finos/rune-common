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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
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

import java.util.Optional;

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
    // Step 3 — type-first resolution with the output-side guard
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

    /** Stub pojo interface carrying its own type-rooted provider — the CSV input/output type. */
    @RuneLabelProvider(labelProvider = TypeLabelProvider.class)
    private interface LabelledRootType {
    }

    /** The generated "…Impl" shape: no annotation of its own, found only via the supertype search. */
    private static class LabelledRootTypeImpl implements LabelledRootType {
    }

    /** A root type with no provider anywhere in its hierarchy — the nested-labels-only case. */
    private static class UnlabelledRootType {
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

    /**
     * An ingest whose function-rooted provider is rooted at its <b>output</b> type, not the serialised
     * input — the §2.7 case the guard exists to reject.
     */
    @Ingest(format = SerializationFormat.CSV_LABELLED)
    @RuneLabelProvider(labelProvider = FunctionLabelProvider.class)
    private static class CsvLabelledIngestWithFunctionProvider implements RosettaFunction {
    }

    @Test
    void csvLabelledProjectionWithRootTypePrefersTheTypeProviderOverTheFunctionProvider() throws JsonProcessingException {
        TransformSerialization s = TransformSerializationResolver.output(CsvLabelledProjectionWithFunctionProvider.class).get();
        ObjectMapper mapper = factory.create(s, CsvLabelledProjectionWithFunctionProvider.class, LabelledRootType.class);

        assertEquals("type:firstName,type:identifier,type:lastName,type:username", header(mapper));
    }

    @Test
    void csvLabelledProjectionWithoutRootTypeStillUsesTheFunctionProvider() throws JsonProcessingException {
        // Regression guard for today's working path: no root type supplied, so resolution falls back to
        // the function's own provider exactly as before Step 3.
        TransformSerialization s = TransformSerializationResolver.output(CsvLabelledProjectionWithFunctionProvider.class).get();
        ObjectMapper mapper = factory.create(s, CsvLabelledProjectionWithFunctionProvider.class, null);

        assertEquals("func:firstName,func:identifier,func:lastName,func:username", header(mapper));
    }

    @Test
    void csvLabelledProjectionWithUnlabelledRootTypeFallsBackToTheFunctionProvider() throws JsonProcessingException {
        // The nested-labels-only shape: the root type carries no type-rooted provider of its own, so the
        // guard must not strip the function's provider from an otherwise-working projection.
        TransformSerialization s = TransformSerializationResolver.output(CsvLabelledProjectionWithFunctionProvider.class).get();
        ObjectMapper mapper = factory.create(s, CsvLabelledProjectionWithFunctionProvider.class, UnlabelledRootType.class);

        assertEquals("func:firstName,func:identifier,func:lastName,func:username", header(mapper));
    }

    @Test
    void csvLabelledIngestWithoutRootTypeGetsNoProviderAndDegradesToPlainCsv() throws JsonProcessingException {
        // §2.7: a function-rooted provider is rooted at the function's OUTPUT, so an ingest (whose
        // serialised side is its INPUT) may never use it — even though the annotation is present, the
        // guard rejects it and there is no root type to try instead.
        TransformSerialization s = TransformSerializationResolver.input(CsvLabelledIngestWithFunctionProvider.class).get();
        ObjectMapper mapper = factory.create(s, CsvLabelledIngestWithFunctionProvider.class, null);

        assertEquals("firstName,identifier,lastName,username", header(mapper));
    }

    @Test
    void csvLabelledIngestWithLabelledInputTypeUsesTheTypeProvider() throws JsonProcessingException {
        // The CSV-import case this whole story exists for: an ingest's function-rooted provider is wrongly
        // rooted at its output, but the caller now supplies the CSV input type as rootType, and type-first
        // resolution picks that up regardless of the (rejected) function provider.
        TransformSerialization s = TransformSerializationResolver.input(CsvLabelledIngestWithFunctionProvider.class).get();
        ObjectMapper mapper = factory.create(s, CsvLabelledIngestWithFunctionProvider.class, LabelledRootType.class);

        assertEquals("type:firstName,type:identifier,type:lastName,type:username", header(mapper));
    }

    @Test
    void csvLabelledResolvesTheTypeProviderThroughAnImplClass() throws JsonProcessingException {
        // Ties Step 1's supertype search to this seam: passing the "…Impl" shape as rootType (rather than
        // the pojo interface itself) must still find the interface's annotation.
        ObjectMapper mapper = factory.create(
                new TransformSerialization(SerializationFormat.CSV_LABELLED, null), null, LabelledRootTypeImpl.class);

        assertEquals("type:firstName,type:identifier,type:lastName,type:username", header(mapper));
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
