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
import com.rosetta.model.lib.annotations.RuneLabelProvider;
import com.rosetta.model.lib.functions.LabelProvider;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.transform.SerializationFormat;
import csv.test.user.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class CachingTransformMapperFactoryTest {

    private final CachingTransformMapperFactory factory =
            new CachingTransformMapperFactory(new ClasspathTransformMapperFactory());

    @Test
    void buildsEachSerializationOnceAndReuses() {
        TransformSerialization json = TransformSerialization.DEFAULT_JSON;
        ObjectMapper first = factory.create(json, null);
        assertSame(first, factory.create(json, null), "an equal serialization must reuse the cached mapper");
        assertSame(first, factory.create(new TransformSerialization(SerializationFormat.JSON, null), null),
                "equality is by value, not identity");
        assertNotSame(first, factory.create(new TransformSerialization(SerializationFormat.CSV, null), null));
    }

    @Test
    void classInsensitiveFormatsShareOneMapperAcrossFunctions() {
        TransformSerialization json = TransformSerialization.DEFAULT_JSON;
        assertSame(factory.create(json, LabelledFunctionA.class), factory.create(json, LabelledFunctionB.class),
                "a JSON mapper does not depend on the function class, so all functions share it");
    }

    @Test
    void csvLabelledIsCachedPerFunctionClass() {
        TransformSerialization labelled = new TransformSerialization(SerializationFormat.CSV_LABELLED, null);
        ObjectMapper forA = factory.create(labelled, LabelledFunctionA.class);
        assertSame(forA, factory.create(labelled, LabelledFunctionA.class),
                "the same labelled function must reuse its cached mapper");
        assertNotSame(forA, factory.create(labelled, LabelledFunctionB.class),
                "labels derive from the function class, so another function must not share the mapper");
    }

    @Test
    void csvLabelledIsCachedPerFunctionClassAndRoot() {
        TransformSerialization labelled = new TransformSerialization(SerializationFormat.CSV_LABELLED, null);
        ObjectMapper forRootA = factory.create(labelled, LabelledFunctionA.class, TransformRoot.output(RootTypeA.class));
        assertSame(forRootA, factory.create(labelled, LabelledFunctionA.class, TransformRoot.output(RootTypeA.class)),
                "the same function and root must reuse the cached mapper");
        assertNotSame(forRootA, factory.create(labelled, LabelledFunctionA.class, TransformRoot.output(RootTypeB.class)),
                "a type-rooted label provider is resolved from the root type, so a different root type "
                        + "must not share the mapper");
        assertNotSame(forRootA, factory.create(labelled, LabelledFunctionA.class),
                "no root at all is its own scope, distinct from a supplied one");
    }

    @Test
    void csvLabelledIsCachedPerTransformSide() {
        // The side decides whether the function-rooted provider survives the guard, so two requests that
        // differ only in side resolve different providers and must not share a mapper.
        TransformSerialization labelled = new TransformSerialization(SerializationFormat.CSV_LABELLED, null);
        assertNotSame(factory.create(labelled, LabelledFunctionA.class, TransformRoot.output(RootTypeA.class)),
                factory.create(labelled, LabelledFunctionA.class, TransformRoot.input(RootTypeA.class)),
                "the transform side changes which provider resolves, so it must be part of the cache scope");
    }

    @Test
    void nonCsvLabelledFormatSharesOneMapperAcrossRoots() {
        TransformSerialization json = TransformSerialization.DEFAULT_JSON;
        assertSame(factory.create(json, LabelledFunctionA.class, TransformRoot.output(RootTypeA.class)),
                factory.create(json, LabelledFunctionA.class, TransformRoot.input(RootTypeB.class)),
                "the root does not affect JSON mapper construction, so it must not affect its cache scope");
    }

    @Test
    void classLoaderSensitiveFormatsShareOneMapperPerClassLoader() {
        TransformSerialization runeJson = new TransformSerialization(SerializationFormat.RUNE_JSON, null);
        assertSame(factory.create(runeJson, LabelledFunctionA.class), factory.create(runeJson, LabelledFunctionB.class),
                "functions loaded by the same classloader must share one RUNE_JSON mapper");
    }

    /**
     * A plain-CSV mapper is cached on {@code (format, configPath)} alone, so once {@code hasHeader}
     * became something a mapper differs by, the question was whether two transforms with different
     * header expectations could collide on one cache entry. They cannot, and the reason is structural
     * rather than a property of this key: a non-default configuration is only reachable through a
     * {@code configPath} — {@code ClasspathTransformMapperFactory.forCsv} short-circuits to
     * {@code RosettaCSVConfiguration.EMPTY}, which is header-bearing, when the path is null or empty —
     * so a transform that expects no header necessarily declares a path, and differs in the key.
     *
     * <p>Asserted by behaviour rather than identity: two paths whose configurations differ only in
     * {@code hasHeader} must produce mappers that write differently. Identity alone would pass on a key
     * that happened to distinguish them for some unrelated reason.</p>
     */
    @Test
    void csvMappersWithDifferentHeaderExpectationsDoNotShareACacheEntry() throws JsonProcessingException {
        TransformSerialization headerless = new TransformSerialization(SerializationFormat.CSV,
                "serialisation/csv/csv-config/headerless-csv-config.json");
        TransformSerialization headerBearing = new TransformSerialization(SerializationFormat.CSV,
                "serialisation/csv/csv-config/semicolon-csv-config.json");
        User user = User.builder()
                .setUsername("asmith")
                .setIdentifier("id-001")
                .setFirstName("Alice")
                .setLastName("Smith")
                .build();

        ObjectMapper first = factory.create(headerless, null);
        assertSame(first, factory.create(headerless, null), "an equal serialization must reuse the cached mapper");
        assertEquals("asmith,id-001,Alice,Smith", firstLine(first, user), "the first line must be data");

        ObjectMapper second = factory.create(headerBearing, null);
        assertNotSame(first, second, "a different configPath must not be served the header-less mapper");
        assertEquals("username;identifier;firstName;lastName", firstLine(second, user),
                "the first line must be column names");

        // And the header-less mapper is still itself afterwards — the second request did not evict
        // or overwrite it.
        assertEquals("asmith,id-001,Alice,Smith", firstLine(factory.create(headerless, null), user));
    }

    private static String firstLine(ObjectMapper mapper, User user) throws JsonProcessingException {
        String csv = mapper.writeValueAsString(user);
        return csv.substring(0, csv.indexOf('\n'));
    }

    @Test
    void clearDropsEveryCachedMapper() {
        TransformSerialization json = TransformSerialization.DEFAULT_JSON;
        ObjectMapper before = factory.create(json, null);
        factory.clear();
        assertNotSame(before, factory.create(json, null),
                "after clear() the mapper must be rebuilt, not served from the stale cache");
    }

    public static class TestLabelProvider implements LabelProvider {
        @Override
        public String getLabel(RosettaPath path) {
            return path.toString();
        }
    }

    @RuneLabelProvider(labelProvider = TestLabelProvider.class)
    private abstract static class LabelledFunctionA implements RosettaFunction {
    }

    @RuneLabelProvider(labelProvider = TestLabelProvider.class)
    private abstract static class LabelledFunctionB implements RosettaFunction {
    }

    private static class RootTypeA {
    }

    private static class RootTypeB {
    }
}
