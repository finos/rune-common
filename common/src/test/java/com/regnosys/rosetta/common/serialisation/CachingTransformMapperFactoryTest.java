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
import com.rosetta.model.lib.RosettaModelObject;
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
     * {@code CSV} takes the same {@code (function class, root)} scope as {@code CSV_LABELLED}, because its
     * configuration decides whether it resolves a {@link LabelProvider} and that cannot be known without
     * loading the config. So two function classes do not share a CSV mapper even within one classloader,
     * wider than this particular request depends on — with no config path the two mappers are identical and
     * the only cost is building them twice.
     */
    @Test
    void csvIsCachedPerFunctionClassAndRoot() {
        TransformSerialization csv = new TransformSerialization(SerializationFormat.CSV, null);
        assertSame(factory.create(csv, LabelledFunctionA.class), factory.create(csv, LabelledFunctionA.class),
                "the same function class and root must reuse the cached mapper");
        assertNotSame(factory.create(csv, LabelledFunctionA.class), factory.create(csv, LabelledFunctionB.class),
                "CSV is scoped to the function class and root, so two function classes must not share an entry");
        assertNotSame(factory.create(csv, LabelledFunctionA.class, TransformRoot.output(RootTypeA.class)),
                factory.create(csv, LabelledFunctionA.class, TransformRoot.input(RootTypeA.class)),
                "the transform side decides whether a function-rooted provider may be used, so the two "
                        + "sides must not share an entry");
    }

    /**
     * The cross-model guarantee, carried by the function class rather than by its classloader: a function
     * class determines its own loader, so scoping to the class is strictly narrower than scoping to the
     * loader and can never serve a mapper built against another model.
     *
     * <p>Only the reachable half is asserted. Two function classes from genuinely different classloaders
     * would need a second classloader to demonstrate, and the ownership rule — one factory per model
     * instance — means production never puts two model classloaders behind one factory.</p>
     */
    @Test
    void csvDoesNotShareAcrossDifferentClassLoaderScopes() {
        TransformSerialization csv = new TransformSerialization(SerializationFormat.CSV, null);
        assertNotSame(factory.create(csv, LabelledFunctionA.class), factory.create(csv, null),
                "a CSV mapper resolved through a function class must not be served to a request that has "
                        + "no function class, and so no classloader, at all");
    }

    /**
     * Two transforms with different header expectations must not collide on one cache entry. The
     * {@code configPath} is the only part of the key that distinguishes these two requests — they carry the
     * same function class and root — and that suffices structurally: a non-default configuration is only
     * reachable through a {@code configPath}, since the factory short-circuits to the header-bearing
     * {@code RosettaCSVConfiguration.EMPTY} when the path is null or empty.
     *
     * <p>Asserted by behaviour rather than identity: two paths whose configurations differ only in
     * {@code hasHeader} must produce mappers that write differently. Identity alone would pass on a key that
     * happened to distinguish them for an unrelated reason.</p>
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

    /**
     * A {@code CSV} mapper is labelled or not according to its configuration, so once a config path is
     * declared it depends on the root exactly as a {@code CSV_LABELLED} mapper does, and two roots must not
     * share an entry — which is why {@code CSV} takes the wider scope even though which configs ask for
     * labels cannot be known when the key is computed.
     *
     * <p>Asserted by behaviour rather than identity: two root types carrying different providers must produce
     * mappers that label differently.</p>
     */
    @Test
    void csvWithALabelConfigIsCachedPerRoot() throws JsonProcessingException {
        TransformSerialization labelConfig = new TransformSerialization(SerializationFormat.CSV,
                "serialisation/csv/csv-config/labelled-semicolon-csv-config.json");
        User user = User.builder().setUsername("asmith").setIdentifier("id-001")
                .setFirstName("Alice").setLastName("Smith").build();

        ObjectMapper forA = factory.create(labelConfig, LabelledFunctionA.class,
                TransformRoot.output(LabelledRootTypeA.class));
        assertSame(forA, factory.create(labelConfig, LabelledFunctionA.class,
                        TransformRoot.output(LabelledRootTypeA.class)),
                "the same function and root must reuse the cached mapper");

        ObjectMapper forB = factory.create(labelConfig, LabelledFunctionA.class,
                TransformRoot.output(LabelledRootTypeB.class));
        assertNotSame(forA, forB, "a different root type resolves a different provider, so it must not share");

        // ':' (58) sits below the ';' (59) separator, so jackson-csv quotes these labels; stripped, since
        // which provider was used is what is under test.
        assertEquals("A:username;A:identifier;A:firstName;A:lastName", firstLine(forA, user).replace("\"", ""));
        assertEquals("B:username;B:identifier;B:firstName;B:lastName", firstLine(forB, user).replace("\"", ""));
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

    /** Distinguishable providers, so a cache-scope test can prove which root type was resolved from. */
    public static class RootALabelProvider implements LabelProvider {
        @Override
        public String getLabel(RosettaPath path) {
            return "A:" + path.buildPath();
        }
    }

    public static class RootBLabelProvider implements LabelProvider {
        @Override
        public String getLabel(RosettaPath path) {
            return "B:" + path.buildPath();
        }
    }

    @RuneLabelProvider(labelProvider = RootALabelProvider.class)
    private interface LabelledRootTypeA extends RosettaModelObject {
    }

    @RuneLabelProvider(labelProvider = RootBLabelProvider.class)
    private interface LabelledRootTypeB extends RosettaModelObject {
    }
}
