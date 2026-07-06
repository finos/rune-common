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
import com.regnosys.rosetta.common.transform.PipelineModel;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.transform.Ingest;
import com.rosetta.model.lib.transform.Projection;
import com.rosetta.model.lib.transform.SerializationFormat;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TransformSerializationResolverTest {

    @Ingest(format = SerializationFormat.XML, configPath = "serialisation/xml/xml-config/my-schema-xml-config.json")
    private static class XmlIngestFunction implements RosettaFunction {
    }

    @Projection(format = SerializationFormat.RUNE_JSON)
    private static class RuneJsonProjectionFunction implements RosettaFunction {
    }

    private static class UnannotatedFunction implements RosettaFunction {
    }

    @Test
    void inputResolvesFromIngestAnnotation() {
        Optional<TransformSerialization> resolved = TransformSerializationResolver.input(XmlIngestFunction.class);
        assertTrue(resolved.isPresent());
        assertEquals(SerializationFormat.XML, resolved.get().getFormat());
        assertEquals("serialisation/xml/xml-config/my-schema-xml-config.json", resolved.get().getConfigPath());
    }

    @Test
    void outputResolvesFromProjectionAnnotation() {
        Optional<TransformSerialization> resolved = TransformSerializationResolver.output(RuneJsonProjectionFunction.class);
        assertTrue(resolved.isPresent());
        assertEquals(SerializationFormat.RUNE_JSON, resolved.get().getFormat());
        assertNull(resolved.get().getConfigPath(), "an empty annotation configPath resolves to null");
    }

    @Test
    void eachSideResolvesOnlyItsOwnAnnotation() {
        assertFalse(TransformSerializationResolver.output(XmlIngestFunction.class).isPresent());
        assertFalse(TransformSerializationResolver.input(RuneJsonProjectionFunction.class).isPresent());
        assertFalse(TransformSerializationResolver.input(UnannotatedFunction.class).isPresent());
        assertFalse(TransformSerializationResolver.output(UnannotatedFunction.class).isPresent());
        assertFalse(TransformSerializationResolver.input(null).isPresent());
    }

    @Test
    @SuppressWarnings("deprecation")
    void annotationWinsOverLegacyPipelineSerialisation() {
        PipelineModel.Serialisation legacyJson =
                new PipelineModel.Serialisation(PipelineModel.Serialisation.Format.JSON, null);
        Optional<TransformSerialization> resolved =
                TransformSerializationResolver.input(XmlIngestFunction.class, legacyJson);
        assertTrue(resolved.isPresent());
        assertEquals(SerializationFormat.XML, resolved.get().getFormat(), "the annotation must win over the legacy config");
    }

    @Ingest(format = SerializationFormat.XML)
    private static class XmlIngestWithoutConfigPath implements RosettaFunction {
    }

    @Test
    @SuppressWarnings("deprecation")
    void legacyConfigPathIsMergedWhenAnnotationHasFormatButNoConfig() {
        // A model generated before annotations carried a config path: @Ingest(XML) with no configPath,
        // while the pipeline serialisation ships the XML config. The format comes from the annotation,
        // the config file from the legacy serialisation.
        PipelineModel.Serialisation legacyXml = new PipelineModel.Serialisation(
                PipelineModel.Serialisation.Format.XML, "xml-config/fpml-config.json");
        Optional<TransformSerialization> resolved =
                TransformSerializationResolver.input(XmlIngestWithoutConfigPath.class, legacyXml);
        assertTrue(resolved.isPresent());
        assertEquals(SerializationFormat.XML, resolved.get().getFormat());
        assertEquals("xml-config/fpml-config.json", resolved.get().getConfigPath(),
                "the legacy config file must not be lost when the annotation carries none");

        // but a legacy config for a DIFFERENT format is not merged — the annotation wins wholesale
        PipelineModel.Serialisation legacyJson = new PipelineModel.Serialisation(
                PipelineModel.Serialisation.Format.JSON, "some/config.json");
        Optional<TransformSerialization> xmlWins =
                TransformSerializationResolver.input(XmlIngestWithoutConfigPath.class, legacyJson);
        assertTrue(xmlWins.isPresent());
        assertEquals(SerializationFormat.XML, xmlWins.get().getFormat());
        assertNull(xmlWins.get().getConfigPath());
    }

    @Test
    @SuppressWarnings("deprecation")
    void legacyPipelineSerialisationAppliesOnlyWhenAnnotationAbsent() {
        PipelineModel.Serialisation legacyXml = new PipelineModel.Serialisation(
                PipelineModel.Serialisation.Format.XML, "some/config.json");
        Optional<TransformSerialization> resolved =
                TransformSerializationResolver.input(UnannotatedFunction.class, legacyXml);
        assertTrue(resolved.isPresent());
        assertEquals(SerializationFormat.XML, resolved.get().getFormat());
        assertEquals("some/config.json", resolved.get().getConfigPath());

        assertFalse(TransformSerializationResolver.input(UnannotatedFunction.class, null).isPresent());
    }

    @Test
    void classpathFactoryConstructsFromResolvedSerialization() {
        ClasspathTransformMapperFactory factory = new ClasspathTransformMapperFactory();
        TransformSerialization xml = TransformSerializationResolver.input(XmlIngestFunction.class).get();
        ObjectMapper mapper = factory.create(xml, XmlIngestFunction.class);
        assertInstanceOf(XmlMapper.class, mapper);

        assertNotNull(factory.createWriter(TransformSerialization.DEFAULT_JSON, null),
                "the default JSON serialization needs no function class");
    }
}
