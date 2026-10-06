package com.regnosys.rosetta.common.serialisation.fix.processor;

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

import com.google.common.collect.ImmutableList;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.functions.LabelProvider;
import com.rosetta.model.lib.path.RosettaPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import quickfix.DataDictionary;
import quickfix.FieldNotFound;
import quickfix.Group;
import quickfix.Message;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RuneFIXSerializerProcessorTest {

    @Mock
    private DataDictionary dataDictionary;

    @Mock
    private LabelProvider labelProvider;

    private RuneFIXSerializerProcessor processor;

    private static final String MSG_TYPE = "AE";

    @BeforeEach
    void setUp() {
        processor = new RuneFIXSerializerProcessor(dataDictionary, labelProvider, MSG_TYPE);
    }

    @Test
    void processBasic_SinglePrimitive_SetsFieldOnMessage() throws FieldNotFound {
        RosettaPath path = mockPath("TradeCaptureReport.firmTradeID.value");
        when(labelProvider.getLabel(any(RosettaPath.class))).thenReturn("FirmTradeID");
        when(dataDictionary.getFieldTag("FirmTradeID")).thenReturn(1041);

        processor.processBasic(path, String.class, "FT-123", null);

        RuneFIXSerializerReport report = processor.report();
        assertFalse(report.hasErrors());
        assertTrue(report.getIssues().isEmpty());

        Message result = report.getFixMessage();
        assertTrue(result.isSetField(1041), "Field 1041 should be set");
        assertEquals("FT-123", result.getString(1041));
    }

    @Test
    void processBasic_PrimitiveCollection_CreatesGroupWithDelimiterPayload() throws FieldNotFound {
        RosettaPath path = mockPath("TradeCaptureReport.side");
        when(labelProvider.getLabel(any(RosettaPath.class))).thenReturn("NoSides");
        when(dataDictionary.getFieldTag("NoSides")).thenReturn(552);

        DataDictionary groupDictionary = mock(DataDictionary.class);
        DataDictionary.GroupInfo groupInfo = mock(DataDictionary.GroupInfo.class);
        when(groupInfo.getDataDictionary()).thenReturn(groupDictionary);
        when(groupDictionary.getOrderedFields()).thenReturn(new int[]{54, 0});
        when(groupInfo.getDelimiterField()).thenReturn(54);
        when(dataDictionary.getGroup(MSG_TYPE, 552)).thenReturn(groupInfo);

        List<String> sides = ImmutableList.of("1", "2");
        processor.processBasic(path, String.class, sides, null);
        assertFalse(processor.report().hasErrors());

        Message result = processor.report().getFixMessage();
        assertTrue(result.isSetField(552));
        assertEquals(2, result.getInt(552));

        Group group1 = result.getGroup(1, 552);
        assertEquals("1", group1.getString(54));

        Group group2 = result.getGroup(2, 552);
        assertEquals("2", group2.getString(54));
    }

    @Test
    void processRosetta_List_CreatesGroupAndProcessesChildren() throws FieldNotFound {
        RosettaPath path = mockPath("TradeCaptureReport.party");
        when(labelProvider.getLabel(any(RosettaPath.class))).thenReturn("NoPartyIDs");
        when(dataDictionary.getFieldTag("NoPartyIDs")).thenReturn(453);

        DataDictionary groupDictionary = mock(DataDictionary.class);
        DataDictionary.GroupInfo groupInfo = mock(DataDictionary.GroupInfo.class);
        when(groupInfo.getDataDictionary()).thenReturn(groupDictionary);
        when(groupDictionary.getOrderedFields()).thenReturn(new int[]{448, 0});
        when(groupInfo.getDelimiterField()).thenReturn(448);
        when(dataDictionary.getGroup(MSG_TYPE, 453)).thenReturn(groupInfo);

        RosettaModelObject mockChild = mock(RosettaModelObject.class);
        List<RosettaModelObject> children = ImmutableList.of(mockChild);

        processor.processRosetta(path, RosettaModelObject.class, children, null);
        Message result = processor.report().getFixMessage();

        assertTrue(result.isSetField(453));
        assertEquals(1, result.getInt(453));

        verify(mockChild, times(1)).process(any(RosettaPath.class), eq(processor));
    }

    @Test
    void processBasic_MissingLabel_RecordsError() throws FieldNotFound {
        RosettaPath path = mockPath("TradeCaptureReport.unknownField");
        when(labelProvider.getLabel(any(RosettaPath.class))).thenReturn(null);

        processor.processBasic(path, String.class, "SomeValue", null);

        RuneFIXSerializerReport report = processor.report();
        assertTrue(report.hasErrors());
        assertEquals(1, report.getErrors().size());
        assertEquals(RuneFIXSerializerIssue.Code.MISSING_LABEL_MAPPING, report.getErrors().get(0).getCode());

        Message result = report.getFixMessage();
        assertEquals(MSG_TYPE, result.getHeader().getString(35));
        assertFalse(result.iterator().hasNext(), "Body should be completely empty");
    }

    @Test
    void processBasic_MissingDictionaryTag_RecordsError() {
        RosettaPath path = mockPath("TradeCaptureReport.customField");
        when(labelProvider.getLabel(any(RosettaPath.class))).thenReturn("CustomLabel");
        when(dataDictionary.getFieldTag("CustomLabel")).thenReturn(-1);

        processor.processBasic(path, String.class, "SomeValue", null);

        RuneFIXSerializerReport report = processor.report();
        assertTrue(report.hasErrors());
        assertEquals(1, report.getErrors().size());
        assertEquals(RuneFIXSerializerIssue.Code.MISSING_FIX_TAG, report.getErrors().get(0).getCode());

        Message result = report.getFixMessage();
        assertFalse(result.iterator().hasNext(), "Body should be completely empty");
    }

    @Test
    void processRosetta_NestedList_UsesParentDictionaryAndPreservesStructure()
            throws FieldNotFound {
        RosettaPath outerPath = mockPath("TradeCaptureReport.side");

        when(labelProvider.getLabel(any(RosettaPath.class))).thenReturn("NoSides", "NoPartyIDs", "PartyID");
        when(dataDictionary.getFieldTag("NoSides")).thenReturn(552);
        when(dataDictionary.getFieldTag("NoPartyIDs")).thenReturn(453);
        when(dataDictionary.getFieldTag("PartyID")).thenReturn(448);

        DataDictionary.GroupInfo sidesInfo = mock(DataDictionary.GroupInfo.class);
        when(sidesInfo.getDelimiterField()).thenReturn(54);

        DataDictionary nestedDictionary = mock(DataDictionary.class);
        when(sidesInfo.getDataDictionary()).thenReturn(nestedDictionary);
        when(nestedDictionary.getOrderedFields()).thenReturn(new int[]{54, 0});

        DataDictionary partyDictionary = mock(DataDictionary.class);
        DataDictionary.GroupInfo partiesInfo = mock(DataDictionary.GroupInfo.class);
        when(partiesInfo.getDataDictionary()).thenReturn(partyDictionary);
        when(partyDictionary.getOrderedFields()).thenReturn(new int[]{448, 447, 452, 0});
        when(partiesInfo.getDelimiterField()).thenReturn(448);

        when(dataDictionary.getGroup(MSG_TYPE, 552)).thenReturn(sidesInfo);
        when(nestedDictionary.getGroup(MSG_TYPE, 453)).thenReturn(partiesInfo);

        RosettaModelObject side = mock(RosettaModelObject.class);
        RosettaModelObject party = mock(RosettaModelObject.class);

        // Configure the outer group to contain a nested party group.
        doAnswer(invocation -> {
            RosettaPath sideItemPath = invocation.getArgument(0);
            processor.processRosetta(
                    RosettaPath.valueOf(sideItemPath.buildPath() + ".party"),
                    RosettaModelObject.class,
                    ImmutableList.of(party),
                    null);

            return null;
        }).when(side).process(any(RosettaPath.class), eq(processor));

        // Configure the nested group to write PartyID into its own Group.
        doAnswer(invocation -> {
            RosettaPath partyItemPath = invocation.getArgument(0);
            processor.processBasic(
                    RosettaPath.valueOf(partyItemPath.buildPath() + ".partyID"),
                    String.class,
                    "PARTY-1",
                    null);

            return null;
        }).when(party).process(any(RosettaPath.class), eq(processor));

        processor.processRosetta(outerPath, RosettaModelObject.class, ImmutableList.of(side), null);

        Message result = processor.report().getFixMessage();
        Group sideGroup = result.getGroup(1, 552);
        Group partyGroup = sideGroup.getGroup(1, 453);

        // Verify the nested FIX group structure and payload.
        assertEquals(1, result.getInt(552));
        assertEquals(1, sideGroup.getInt(453));
        assertEquals("PARTY-1", partyGroup.getString(448));

        // Verify nested metadata is resolved from the parent group dictionary.
        verify(dataDictionary).getGroup(MSG_TYPE, 552);
        verify(nestedDictionary).getGroup(MSG_TYPE, 453);
        verify(dataDictionary, never()).getGroup(MSG_TYPE, 453);
    }

    @Test
    void processRosetta_List_MissingGroupMetadata_RecordsError() {
        RosettaPath path = mockPath("TradeCaptureReport.party");
        when(labelProvider.getLabel(any(RosettaPath.class))).thenReturn("NoPartyIDs");
        when(dataDictionary.getFieldTag("NoPartyIDs")).thenReturn(453);
        when(dataDictionary.getGroup(MSG_TYPE, 453)).thenReturn(null);

        RosettaModelObject child = mock(RosettaModelObject.class);

        processor.processRosetta(path, RosettaModelObject.class, ImmutableList.of(child), null);

        RuneFIXSerializerReport report = processor.report();
        assertFalse(report.getFixMessage().isSetField(453));
        assertTrue(report.hasErrors());
        assertEquals(1, report.getErrors().size());
        assertEquals(RuneFIXSerializerIssue.Code.MISSING_GROUP_METADATA, report.getErrors().get(0).getCode());
    }

    @Test
    void processBasic_PrimitiveCollection_MissingGroupMetadata_RecordsError() {
        RosettaPath path = mockPath("TradeCaptureReport.side");
        when(labelProvider.getLabel(any(RosettaPath.class))).thenReturn("NoSides");
        when(dataDictionary.getFieldTag("NoSides")).thenReturn(552);
        when(dataDictionary.getGroup(MSG_TYPE, 552)).thenReturn(null);

        processor.processBasic(path, String.class, ImmutableList.of("1"), null);

        RuneFIXSerializerReport report = processor.report();
        assertFalse(report.getFixMessage().isSetField(552));
        assertTrue(report.hasErrors());
        assertEquals(1, report.getErrors().size());
        assertEquals(RuneFIXSerializerIssue.Code.MISSING_GROUP_METADATA, report.getErrors().get(0).getCode());
    }

    @Test
    void processRosetta_List_UsesDictionaryOrderForGroupMembers() {
        RosettaPath path = mockPath("TradeCaptureReport.party");

        when(labelProvider.getLabel(any(RosettaPath.class))).thenReturn("NoPartyIDs", "PartyIDSource", "PartyID");
        when(dataDictionary.getFieldTag("NoPartyIDs")).thenReturn(453);
        when(dataDictionary.getFieldTag("PartyIDSource")).thenReturn(447);
        when(dataDictionary.getFieldTag("PartyID")).thenReturn(448);

        DataDictionary groupDictionary = mock(DataDictionary.class);
        DataDictionary.GroupInfo groupInfo = mock(DataDictionary.GroupInfo.class);
        when(groupInfo.getDelimiterField()).thenReturn(448);
        when(groupInfo.getDataDictionary()).thenReturn(groupDictionary);
        when(groupDictionary.getOrderedFields()).thenReturn(new int[]{448, 447, 0});
        when(dataDictionary.getGroup(MSG_TYPE, 453)).thenReturn(groupInfo);

        RosettaModelObject child = mock(RosettaModelObject.class);

        doAnswer(invocation -> {
            RosettaPath itemPath = invocation.getArgument(0);

            processor.processBasic(
                    RosettaPath.valueOf(itemPath.buildPath() + ".partyIDSource"),
                    String.class,
                    "D",
                    null);

            processor.processBasic(
                    RosettaPath.valueOf(itemPath.buildPath() + ".partyID"),
                    String.class,
                    "PARTY-1",
                    null);
            return null;
        }).when(child).process(any(RosettaPath.class), eq(processor));

        processor.processRosetta(
                path,
                RosettaModelObject.class,
                ImmutableList.of(child),
                null);

        String serialized = processor.report().getFixMessage().toString();
        assertTrue(serialized.indexOf("448=PARTY-1") < serialized.indexOf("447=D"));
    }

    /**
     * Helper to safely create RosettaPaths for the processor to extract relative names.
     */
    private RosettaPath mockPath(String absolutePath) {
        return RosettaPath.valueOf(absolutePath);
    }
}
