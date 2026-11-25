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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.regnosys.rosetta.common.serialisation.runejson.config.RuneJSONConfiguration;
import com.rosetta.model.lib.transform.Ingest;
import com.rosetta.model.lib.transform.Projection;
import com.rosetta.model.lib.transform.SerializationFormat;
import com.rosetta.model.metafields.FieldWithMetaString;
import com.rosetta.model.metafields.MetaFields;
import csv.test.metadata.MetadataAttributeHolder;
import org.finos.rune.mapper.RuneJsonObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClasspathTransformMapperFactoryRuneJsonTest {

    private static final String ATTRIBUTES_ONLY_CONFIG = "serialisation/runejson/attributes-only-config.json";
    private static final String NOT_ATTRIBUTES_ONLY_CONFIG = "serialisation/runejson/not-attributes-only-config.json";
    private static final String EMPTY_CONFIG = "serialisation/runejson/empty-config.json";

    private final ClasspathTransformMapperFactory factory = new ClasspathTransformMapperFactory();

    @Projection(format = SerializationFormat.RUNE_JSON, configPath = ATTRIBUTES_ONLY_CONFIG)
    private static class AttributesOnlyProjection {
    }

    @Ingest(format = SerializationFormat.RUNE_JSON, configPath = ATTRIBUTES_ONLY_CONFIG)
    private static class AttributesOnlyIngest {
    }

    private static TransformSerialization newRuneJsonSerialization(String configPath) {
        return new TransformSerialization(SerializationFormat.RUNE_JSON, configPath);
    }

    private static MetadataAttributeHolder newHolderWithScheme() {
        return MetadataAttributeHolder.builder()
                .setHead("h1")
                .setScheme(FieldWithMetaString.builder()
                        .setValue("S1")
                        .setMeta(MetaFields.builder().setScheme("https://www.example.com/scheme")))
                .setTail("t1")
                .build();
    }

    private ObjectMapper createOutputMapper(String configPath) {
        return factory.create(newRuneJsonSerialization(configPath), AttributesOnlyProjection.class, TransformRoot.output());
    }

    @Test
    void attributesOnlyProjectionWritesOnlyModelAttributes() throws Exception {
        ObjectMapper mapper = TransformSerializationResolver.output(AttributesOnlyProjection.class)
                .map(s -> factory.create(s, AttributesOnlyProjection.class, TransformRoot.output()))
                .get();

        assertEquals("{\"head\":\"h1\",\"scheme\":\"S1\",\"tail\":\"t1\"}", mapper.writeValueAsString(newHolderWithScheme()));
    }

    @Test
    void attributesOnlyProjectionWriterWritesOnlyModelAttributes() throws Exception {
        String json = factory.createWriter(newRuneJsonSerialization(ATTRIBUTES_ONLY_CONFIG), AttributesOnlyProjection.class, TransformRoot.output())
                .writeValueAsString(newHolderWithScheme());

        assertFalse(json.contains("@"), json);
    }

    @Test
    void noConfigPathWritesStandardRuneJson() throws Exception {
        assertStandardRuneJson(createOutputMapper(null));
    }

    @Test
    void attributesOnlyFalseWritesStandardRuneJson() throws Exception {
        assertStandardRuneJson(createOutputMapper(NOT_ATTRIBUTES_ONLY_CONFIG));
    }

    @Test
    void absentAttributesOnlyWritesStandardRuneJson() throws Exception {
        assertStandardRuneJson(createOutputMapper(EMPTY_CONFIG));
    }

    private static void assertStandardRuneJson(ObjectMapper mapper) throws Exception {
        assertFalse(((RuneJsonObjectMapper) mapper).isAttributesOnly());
        assertEquals(new RuneJsonObjectMapper().writeValueAsString(newHolderWithScheme()),
                mapper.writeValueAsString(newHolderWithScheme()));
    }

    @Test
    void attributesOnlyMapperBuiltForIngestRefusesToRead() {
        TransformSerialization serialization = TransformSerializationResolver.input(AttributesOnlyIngest.class).get();
        ObjectMapper mapper = factory.create(serialization, AttributesOnlyIngest.class, TransformRoot.input());

        assertInstanceOf(RuneJsonObjectMapper.class, mapper);
        InvalidDefinitionException error = assertThrows(InvalidDefinitionException.class,
                () -> mapper.readValue("{\"head\":\"h1\"}", MetadataAttributeHolder.class));
        assertTrue(error.getMessage().contains("attributes-only Rune JSON"), error.getMessage());
    }

    @Test
    void openConfigOverrideSuppliesRuneJsonConfig() throws Exception {
        List<SerializationFormat> formats = new ArrayList<>();
        ClasspathTransformMapperFactory workspaceFactory = new ClasspathTransformMapperFactory() {
            @Override
            protected InputStream openConfig(SerializationFormat format, String configPath, Class<?> functionClass) {
                formats.add(format);
                return new ByteArrayInputStream("{\"attributesOnly\": true}".getBytes(StandardCharsets.UTF_8));
            }
        };

        ObjectMapper mapper = workspaceFactory.create(newRuneJsonSerialization("workspace/not-on-classpath.json"),
                AttributesOnlyProjection.class, TransformRoot.output());

        assertEquals(Arrays.asList(SerializationFormat.RUNE_JSON), formats);
        assertEquals("{\"head\":\"h1\",\"scheme\":\"S1\",\"tail\":\"t1\"}", mapper.writeValueAsString(newHolderWithScheme()));
    }

    @Test
    void missingConfigFails() {
        String configPath = "serialisation/runejson/does-not-exist.json";
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> createOutputMapper(configPath));

        assertTrue(error.getMessage().contains(configPath), error.getMessage());
    }

    @Test
    void invalidConfigFails() {
        assertFailsToRead("serialisation/runejson/invalid-config.json");
    }

    @Test
    void nonBooleanSettingFails() {
        assertFailsToRead("serialisation/runejson/non-boolean-config.json");
    }

    @Test
    void failedConfigIsNotCached() {
        CachingTransformMapperFactory cachingFactory = new CachingTransformMapperFactory(factory);
        String configPath = "serialisation/runejson/invalid-config.json";

        assertThrows(UncheckedIOException.class,
                () -> cachingFactory.create(newRuneJsonSerialization(configPath), AttributesOnlyProjection.class, TransformRoot.output()));
        assertThrows(UncheckedIOException.class,
                () -> cachingFactory.create(newRuneJsonSerialization(configPath), AttributesOnlyProjection.class, TransformRoot.output()));
    }

    private void assertFailsToRead(String configPath) {
        UncheckedIOException error = assertThrows(UncheckedIOException.class, () -> createOutputMapper(configPath));

        assertTrue(error.getMessage().contains(configPath), error.getMessage());
    }

    @Test
    void misspeltSettingWarnsAndWritesStandardRuneJson() throws Exception {
        String configPath = "serialisation/runejson/misspelt-config.json";
        List<ObjectMapper> mapper = new ArrayList<>();
        List<String> warnings = collectWarningsWhile(() -> mapper.add(createOutputMapper(configPath)));

        assertEquals(1, warnings.size(), "expected exactly one WARN, got " + warnings);
        assertTrue(warnings.get(0).contains(configPath), warnings.get(0));
        assertTrue(warnings.get(0).contains("[attributeOnly]"), warnings.get(0));
        assertStandardRuneJson(mapper.get(0));
    }

    /** The WARNs logged by the factory and the configuration loader while {@code work} runs. */
    private static List<String> collectWarningsWhile(Runnable work) {
        List<Logger> loggers = Arrays.asList(
                (Logger) LoggerFactory.getLogger(ClasspathTransformMapperFactory.class),
                (Logger) LoggerFactory.getLogger(RuneJSONConfiguration.class));
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        loggers.forEach(logger -> logger.addAppender(appender));
        try {
            work.run();
        } finally {
            loggers.forEach(logger -> logger.detachAppender(appender));
            appender.stop();
        }
        return appender.list.stream()
                .filter(event -> event.getLevel() == Level.WARN)
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
    }
}
