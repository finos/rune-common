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
        ObjectMapper mapper = TransformObjectMapperFactory.forTransformFunction(XmlSchemaIngest.class);
        assertNotNull(mapper);
        assertInstanceOf(XmlMapper.class, mapper);
    }

    @Test
    void buildsXmlMapperForBareFormatWithoutConfigPath() {
        ObjectMapper mapper = TransformObjectMapperFactory.forTransformFunction(BareXmlIngest.class);
        assertNotNull(mapper);
        assertInstanceOf(XmlMapper.class, mapper);
    }

    @Test
    void buildsJsonMapperForJsonIngest() {
        ObjectMapper mapper = TransformObjectMapperFactory.forTransformFunction(JsonIngest.class);
        assertNotNull(mapper);
    }

    @Test
    void buildsRuneJsonMapperForRuneJsonProjection() {
        ObjectMapper mapper = TransformObjectMapperFactory.forTransformFunction(RuneJsonProjection.class);
        assertInstanceOf(RuneJsonObjectMapper.class, mapper);
    }

    @Test
    void buildsCsvMapperForCsvProjection() {
        ObjectMapper mapper = TransformObjectMapperFactory.forTransformFunction(CsvProjection.class);
        assertNotNull(mapper);
    }

    @Test
    void buildsCsvMapperForCsvLabelledProjectionUsingAnnotatedLabelProvider() {
        ObjectMapper mapper = TransformObjectMapperFactory.forTransformFunction(CsvLabelledProjection.class);
        assertNotNull(mapper);
    }

    @Test
    void csvLabelledWithoutRuneLabelProviderIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> TransformObjectMapperFactory.forTransformFunction(CsvLabelledProjectionWithoutLabelProvider.class));
        assertTrue(e.getMessage().contains("@RuneLabelProvider"));
    }

    @Test
    void csvLabelledCannotBeBuiltFromFormatAlone() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> TransformObjectMapperFactory.create(SerializationFormat.CSV_LABELLED, null,
                        TransformObjectMapperFactoryTest.class.getClassLoader()));
        assertTrue(e.getMessage().contains("CSV_LABELLED"));
    }

    @Test
    void forIngestReadsAnnotationDirectly() {
        Ingest ingest = JsonIngest.class.getAnnotation(Ingest.class);
        assertNotNull(TransformObjectMapperFactory.forIngest(ingest, JsonIngest.class.getClassLoader()));
    }

    @Test
    void forProjectionReadsAnnotationDirectly() {
        Projection projection = CsvProjection.class.getAnnotation(Projection.class);
        assertNotNull(TransformObjectMapperFactory.forProjection(projection, CsvProjection.class.getClassLoader()));
    }

    @Test
    void enrichTransformHasNoObjectMapper() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> TransformObjectMapperFactory.forTransformFunction(Enricher.class));
        assertTrue(e.getMessage().contains("@Enrich"));
    }

    @Test
    void unannotatedClassIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> TransformObjectMapperFactory.forTransformFunction(NotAnnotated.class));
        assertTrue(e.getMessage().contains("not annotated"));
    }

    @Test
    void missingXmlConfigResourceIsReported() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> TransformObjectMapperFactory.create(SerializationFormat.XML, "does/not/exist.json",
                        TransformObjectMapperFactoryTest.class.getClassLoader()));
        assertTrue(e.getMessage().contains("does/not/exist.json"));
    }
}
