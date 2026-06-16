package com.regnosys.rosetta.common.serialisation.runejson;

/*-
 * ==============
 * Rune Common
 * ==============
 * Copyright (C) 2018 - 2024 REGnosys
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
import com.regnosys.rosetta.common.hashing.ReferenceConfig;
import com.regnosys.rosetta.common.hashing.ReferenceResolverProcessStep;
import com.regnosys.rosetta.tests.RosettaInjectorProvider;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.test.AttributeRef;
import com.rosetta.test.NodeRef;
import com.rosetta.test.Root;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.finos.rune.mapper.RuneJsonObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Path;
import java.nio.file.Paths;

import static com.regnosys.rosetta.common.util.ResourceUtils.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(InjectionExtension.class)
@InjectWith(RosettaInjectorProvider.class)
public class RuneJsonSerialisationTest {

    private static final Path RESOURCES_PATH = Paths.get("src/test/resources");
    private static final Path RUNE_JSON_TEST_RESOURCES_PATH = RESOURCES_PATH.resolve(Paths.get("serialisation/json/input"));

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new RuneJsonObjectMapper();
    }
    
    @Test
    void testSerialisationWithResolvedNodeReference() {
        String input = readAsString(RUNE_JSON_TEST_RESOURCES_PATH.resolve("node-key-with-ref.json"));

        Root deserializedObject = fromJson(objectMapper, input, Root.class);

        // assert that the reference is unresolved
        NodeRef deserializedNodeRef = deserializedObject.getNodeRef();
        assertNotNull(deserializedNodeRef.getTypeA());
        assertNull(deserializedNodeRef.getAReference().getValue());
        
        Root resolvedObject = resolveReferences(deserializedObject);

        // assert that the reference is resolved
        NodeRef resolvedNodeRef = resolvedObject.getNodeRef();
        assertEquals(resolvedNodeRef.getTypeA(), resolvedNodeRef.getAReference().getValue());
        
        // assert the resolved reference is not serialised
        String result = toJson(objectMapper, resolvedObject);
        assertEquals(input, result);
    }

    @Test
    void testSerialisationWithResolvedAttributeReference() {
        String input = readAsString(RUNE_JSON_TEST_RESOURCES_PATH.resolve("attribute-key-with-ref.json"));

        Root deserializedObject = fromJson(objectMapper, input, Root.class);

        // assert that the reference is unresolved
        AttributeRef deserializedAttributeRef = deserializedObject.getAttributeRef();
        assertNotNull(deserializedAttributeRef.getDateField().getValue());
        assertNull(deserializedAttributeRef.getDateReference().getValue());
        
        Root resolvedObject = resolveReferences(deserializedObject);
        
        // assert that the reference was resolved
        AttributeRef attributeRef = resolvedObject.getAttributeRef();
        assertEquals(attributeRef.getDateField().getValue(), attributeRef.getDateReference().getValue());

        // assert the resolved reference is not serialised
        String result = toJson(objectMapper, resolvedObject);
        assertEquals(input, result);
    }

    private static <T extends RosettaModelObject> T resolveReferences(T o) {
        return (T) new ReferenceResolverProcessStep(ReferenceConfig.noScopeOrExcludedPaths())
                .runProcessStep(o.getType(), o)
                .getResultObject();
    }
}
