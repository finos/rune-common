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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransformObjectMapperFactoryTest {

    private static final String XML_CONFIG = "serialisation/xml/xml-config/extension-schema-xml-config.json";

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
        ObjectMapper mapper = TransformObjectMapperFactory.inputForTransformFunction(XmlSchemaIngest.class).get();
        assertInstanceOf(XmlMapper.class, mapper);
    }

    @Test
    void buildsXmlMapperForBareFormatWithoutConfigPath() {
        ObjectMapper mapper = TransformObjectMapperFactory.inputForTransformFunction(BareXmlIngest.class).get();
        assertInstanceOf(XmlMapper.class, mapper);
    }

    @Test
    void buildsJsonMapperForJsonIngest() {
        assertTrue(TransformObjectMapperFactory.inputForTransformFunction(JsonIngest.class).isPresent());
    }

    @Test
    void buildsRuneJsonMapperForRuneJsonProjection() {
        ObjectMapper mapper = TransformObjectMapperFactory.outputForTransformFunction(RuneJsonProjection.class).get();
        assertInstanceOf(RuneJsonObjectMapper.class, mapper);
    }

    @Test
    void buildsCsvMapperForCsvProjection() {
        assertTrue(TransformObjectMapperFactory.outputForTransformFunction(CsvProjection.class).isPresent());
    }

    @Test
    void buildsCsvMapperForCsvLabelledProjectionUsingAnnotatedLabelProvider() {
        assertTrue(TransformObjectMapperFactory.outputForTransformFunction(CsvLabelledProjection.class).isPresent());
    }

    @Test
    void csvLabelledWithoutRuneLabelProviderFallsBackToPlainCsv() {
        // No @RuneLabelProvider (e.g. a non-generated function): degrade to plain CSV rather than fail.
        assertTrue(TransformObjectMapperFactory.outputForTransformFunction(CsvLabelledProjectionWithoutLabelProvider.class).isPresent());
    }

    @Test
    void csvLabelledFromFormatAloneFallsBackToPlainCsv() {
        // Built from the format alone (no function class -> no label provider): plain CSV, no exception.
        ObjectMapper mapper = TransformObjectMapperFactory.create(SerializationFormat.CSV_LABELLED, null,
                TransformObjectMapperFactoryTest.class.getClassLoader());
        assertNotNull(mapper);
    }

    @Test
    void enrichTransformHasNoObjectMapper() {
        assertFalse(TransformObjectMapperFactory.inputForTransformFunction(Enricher.class).isPresent());
        assertFalse(TransformObjectMapperFactory.outputForTransformFunction(Enricher.class).isPresent());
    }

    @Test
    void unannotatedClassHasNoObjectMapper() {
        assertFalse(TransformObjectMapperFactory.inputForTransformFunction(NotAnnotated.class).isPresent());
        assertFalse(TransformObjectMapperFactory.outputForTransformFunction(NotAnnotated.class).isPresent());
    }

    @Test
    void inputAndOutputResolveOnlyTheirOwnSide() {
        // an @Ingest class has an input mapper but no output mapper, and vice-versa
        assertTrue(TransformObjectMapperFactory.inputForTransformFunction(JsonIngest.class).isPresent());
        assertFalse(TransformObjectMapperFactory.outputForTransformFunction(JsonIngest.class).isPresent());
        assertTrue(TransformObjectMapperFactory.outputForTransformFunction(CsvProjection.class).isPresent());
        assertFalse(TransformObjectMapperFactory.inputForTransformFunction(CsvProjection.class).isPresent());
    }

    @Test
    void missingXmlConfigResourceIsReported() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> TransformObjectMapperFactory.create(SerializationFormat.XML, "does/not/exist.json",
                        TransformObjectMapperFactoryTest.class.getClassLoader()));
        assertTrue(e.getMessage().contains("does/not/exist.json"));
    }
}
