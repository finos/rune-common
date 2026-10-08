package com.regnosys.rosetta.common.serialisation.fix;

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

import com.google.common.collect.ImmutableMap;
import com.regnosys.rosetta.common.transform.LabelProviderResolver;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.functions.LabelProvider;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.Processor;
import com.regnosys.rosetta.common.serialisation.fix.processor.RuneFIXSerializerProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import quickfix.DataDictionary;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RuneFIXMapperTest {

    private static final String TEST_DICTIONARY_PATH = "FIX50SP2.xml";

    @Mock
    private DataDictionary dataDictionary;

    @Mock
    private LabelProvider labelProvider;

    private RuneFIXMapper fixMapper;

    @BeforeEach
    void setUp() {
        RuneFIXConfiguration configuration = RuneFIXConfiguration.builder()
                .setDictionaryPath(TEST_DICTIONARY_PATH)
                .setMessageTypeRouting(ImmutableMap.of("DummyModel", "AE"))
                .build();
        fixMapper = RuneFIXMapper.createFIXMapper(configuration, dataDictionary);
    }

    @Test
    void testCreateFIXMapper_ThrowsOnNullInputs() {
        RuneFIXConfiguration configuration = RuneFIXConfiguration.builder()
                .setDictionaryPath(TEST_DICTIONARY_PATH)
                .setMessageTypeRouting(ImmutableMap.of("DummyModel", "AE"))
                .build();

        assertThrows(IllegalArgumentException.class, () -> RuneFIXMapper.createFIXMapper(null, dataDictionary),
                "Should reject null configuration");

        assertThrows(
                IllegalArgumentException.class, () -> RuneFIXMapper.createFIXMapper(configuration, null),
                "Should reject null dictionary");
    }

    @Test
    void testFixMapperSerialize_PrimitiveFields_UsesConfiguredMsgType() {
        when(labelProvider.getLabel(any())).thenAnswer(invocation -> {
            Object path = invocation.getArgument(0);
            if (path.toString().contains("username")) return "Username";
            if (path.toString().contains("identifier")) return "Identifier";
            return null;
        });

        when(dataDictionary.getFieldTag("Username")).thenReturn(555);
        when(dataDictionary.getFieldTag("Identifier")).thenReturn(556);

        DummyModel dummyModel = mock(DummyModel.class);
        when(dummyModel.getType()).thenReturn((Class) DummyModel.class);

        doAnswer(invocation -> {
            Processor processor = invocation.getArgument(1);

            // Process the username field
            RosettaPath userPath = RosettaPath.valueOf("TradeCaptureReport.username");
            processor.processBasic(userPath, String.class, "testUser", null);

            // Process the identifier field
            RosettaPath idPath = RosettaPath.valueOf("TradeCaptureReport.identifier");
            processor.processBasic(idPath, String.class, "ID-123", null);

            return null;
        }).when(dummyModel).process(any(RosettaPath.class), any(Processor.class));

        String serialized;

        try (MockedStatic<LabelProviderResolver> mockedStatic = mockStatic(LabelProviderResolver.class)) {
            mockedStatic.when(() -> LabelProviderResolver.fromType(any())).thenReturn(labelProvider);
            serialized = fixMapper.writeValueAsString(dummyModel);
        }

        assertNotNull(serialized, "Serialized FIX string should not be null");
        assertTrue(serialized.contains("35=AE\u0001"), "Missing or malformed MsgType (Tag 35)");
        assertTrue(serialized.contains("555=testUser\u0001"), "Missing primitive field (Tag 555)");
        assertTrue(serialized.contains("556=ID-123\u0001"), "Missing primitive field (Tag 556)");
    }

    @Test
    void testFixMapperSerialize_CustomMsgTypeRouting() {

        RuneFIXConfiguration customConfig = RuneFIXConfiguration.builder()
                .setDictionaryPath(TEST_DICTIONARY_PATH)
                .setMessageTypeRouting(ImmutableMap.of("DummyModel", "XX"))
                .build();

        RuneFIXMapper customMapper = RuneFIXMapper.createFIXMapper(customConfig, dataDictionary);

        DummyModel dummyModel = mock(DummyModel.class);
        when(dummyModel.getType()).thenReturn((Class) DummyModel.class);

        doAnswer(invocation -> null).when(dummyModel).process(any(), any(RuneFIXSerializerProcessor.class));

        String serialized;
        try (MockedStatic<LabelProviderResolver> mockedStatic = mockStatic(LabelProviderResolver.class)) {
            mockedStatic.when(() -> LabelProviderResolver.fromType(any())).thenReturn(labelProvider);
            serialized = customMapper.writeValueAsString(dummyModel);
        }

        assertNotNull(serialized, "Serialized FIX string should not be null");
        assertTrue(serialized.contains("35=XX\u0001"), "Missing or malformed custom MsgType (Tag 35)");
    }

    @Test
    void testFixMapperDeserialize_ThrowsUnsupportedOperationException() {
        String input = "9=50\u000135=BE\u0001555=testUser\u000110=123\u0001";

        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () -> {
            fixMapper.readValue(input, RosettaModelObject.class);
        });
        assertEquals("FIX to Rune deserialization is pending implementation.", exception.getMessage());
    }

    /**
     * Stub class used to test primitive field serialization and routing without triggering Mockito proxy name errors.
     */
    private abstract static class DummyModel implements RosettaModelObject {}
}
