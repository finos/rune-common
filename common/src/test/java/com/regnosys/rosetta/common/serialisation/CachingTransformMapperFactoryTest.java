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
import com.rosetta.model.lib.annotations.RuneLabelProvider;
import com.rosetta.model.lib.functions.LabelProvider;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.transform.SerializationFormat;
import org.junit.jupiter.api.Test;

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
    void classLoaderSensitiveFormatsShareOneMapperPerClassLoader() {
        TransformSerialization runeJson = new TransformSerialization(SerializationFormat.RUNE_JSON, null);
        assertSame(factory.create(runeJson, LabelledFunctionA.class), factory.create(runeJson, LabelledFunctionB.class),
                "functions loaded by the same classloader must share one RUNE_JSON mapper");
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
}
