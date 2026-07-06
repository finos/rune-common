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
    void csvLabelledIsNotCached() {
        TransformSerialization labelled = new TransformSerialization(SerializationFormat.CSV_LABELLED, null);
        // labels derive from the function class, so each call constructs (here: unlabelled fallback)
        assertNotSame(factory.create(labelled, null), factory.create(labelled, null));
    }
}
