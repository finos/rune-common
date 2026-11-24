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
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.rosetta.model.lib.transform.Ingest;
import com.rosetta.model.lib.transform.Projection;
import com.rosetta.model.lib.transform.SerializationFormat;
import com.rosetta.model.metafields.FieldWithMetaString;
import com.rosetta.model.metafields.MetaFields;
import csv.test.metadata.MetadataAttributeHolder;
import org.finos.rune.mapper.RuneJsonObjectMapper;
import org.junit.jupiter.api.Test;

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

    private static TransformSerialization runeJson(String configPath) {
        return new TransformSerialization(SerializationFormat.RUNE_JSON, configPath);
    }

    private static MetadataAttributeHolder holderWithScheme() {
        return MetadataAttributeHolder.builder()
                .setHead("h1")
                .setScheme(FieldWithMetaString.builder()
                        .setValue("S1")
                        .setMeta(MetaFields.builder().setScheme("https://www.example.com/scheme")))
                .setTail("t1")
                .build();
    }

    private ObjectMapper outputMapper(String configPath) {
        return factory.create(runeJson(configPath), AttributesOnlyProjection.class, TransformRoot.output());
    }

    @Test
    void attributesOnlyProjectionWritesOnlyModelAttributes() throws Exception {
        ObjectMapper mapper = TransformSerializationResolver.output(AttributesOnlyProjection.class)
                .map(s -> factory.create(s, AttributesOnlyProjection.class, TransformRoot.output()))
                .get();

        assertEquals("{\"head\":\"h1\",\"scheme\":\"S1\",\"tail\":\"t1\"}", mapper.writeValueAsString(holderWithScheme()));
    }

    @Test
    void attributesOnlyProjectionWriterWritesOnlyModelAttributes() throws Exception {
        String json = factory.createWriter(runeJson(ATTRIBUTES_ONLY_CONFIG), AttributesOnlyProjection.class, TransformRoot.output())
                .writeValueAsString(holderWithScheme());

        assertFalse(json.contains("@"), json);
    }

    @Test
    void noConfigPathWritesStandardRuneJson() throws Exception {
        assertStandardRuneJson(outputMapper(null));
    }

    @Test
    void attributesOnlyFalseWritesStandardRuneJson() throws Exception {
        assertStandardRuneJson(outputMapper(NOT_ATTRIBUTES_ONLY_CONFIG));
    }

    @Test
    void absentAttributesOnlyWritesStandardRuneJson() throws Exception {
        assertStandardRuneJson(outputMapper(EMPTY_CONFIG));
    }

    private static void assertStandardRuneJson(ObjectMapper mapper) throws Exception {
        assertFalse(((RuneJsonObjectMapper) mapper).isAttributesOnly());
        assertEquals(new RuneJsonObjectMapper().writeValueAsString(holderWithScheme()),
                mapper.writeValueAsString(holderWithScheme()));
    }

    @Test
    void attributesOnlyIngestFailsWhenMapperIsBuilt() {
        TransformSerialization serialization = TransformSerializationResolver.input(AttributesOnlyIngest.class).get();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> factory.create(serialization, AttributesOnlyIngest.class, TransformRoot.input()));

        assertTrue(error.getMessage().contains(ATTRIBUTES_ONLY_CONFIG), error.getMessage());
        assertTrue(error.getMessage().contains("cannot be used to ingest"), error.getMessage());
    }

    @Test
    void attributesOnlyMapperWithNoDeclaredSideFailsToRead() {
        ObjectMapper mapper = factory.create(runeJson(ATTRIBUTES_ONLY_CONFIG), null, null);

        assertInstanceOf(RuneJsonObjectMapper.class, mapper);
        InvalidDefinitionException error = assertThrows(InvalidDefinitionException.class,
                () -> mapper.readValue("{\"head\":\"h1\"}", MetadataAttributeHolder.class));
        assertTrue(error.getMessage().contains("attributes-only Rune JSON"), error.getMessage());
    }

    @Test
    void cachedProjectionMapperIsNotServedToIngest() {
        CachingTransformMapperFactory cachingFactory = new CachingTransformMapperFactory(factory);
        cachingFactory.create(runeJson(ATTRIBUTES_ONLY_CONFIG), AttributesOnlyProjection.class, TransformRoot.output());

        assertThrows(IllegalArgumentException.class,
                () -> cachingFactory.create(runeJson(ATTRIBUTES_ONLY_CONFIG), AttributesOnlyIngest.class, TransformRoot.input()));
    }

    @Test
    void missingConfigFails() {
        assertThrows(IllegalStateException.class, () -> outputMapper("serialisation/runejson/does-not-exist.json"));
    }
}
