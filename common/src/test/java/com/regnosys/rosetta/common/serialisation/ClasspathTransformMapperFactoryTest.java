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
import org.finos.rune.mapper.RuneJsonObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Optional;

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
}
