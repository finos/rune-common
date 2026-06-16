package com.regnosys.rosetta.common.serialisation.xml;

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

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapperCreator;
import com.regnosys.rosetta.common.serialisation.xml.config.OccursMax;
import com.regnosys.rosetta.common.serialisation.xml.config.RosettaXMLConfiguration;
import com.regnosys.rosetta.common.serialisation.xml.config.TypeXMLConfiguration;
import com.regnosys.rosetta.common.serialisation.xml.config.XMLContentModel;
import com.regnosys.rosetta.common.serialisation.xml.config.XMLContentModelNodeType;
import com.rosetta.model.lib.ModelSymbolId;
import com.rosetta.test.AllVirtualContainer;
import com.rosetta.test.AnyVirtualContainer;
import com.rosetta.test.FpmlFxTargetKnockoutForward;
import com.rosetta.test.FpmlTradeIdentifier;
import com.rosetta.test.MultiLayerContainer;
import com.rosetta.test.MultiLeafContainer;
import com.rosetta.util.DottedPath;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the XML content-model disambiguation runtime.
 *
 * <p>Covers the four worked examples in
 * {@code XML_CONTENT_MODEL_DISAMBIGUATION_DESIGN.md}, plus the two failure cases.</p>
 */
public class XmlContentModelDisambiguationTest {

    private final ObjectMapper xmlMapper;

    public XmlContentModelDisambiguationTest() throws IOException {
        try (InputStream inputStream = Resources.getResource(
                "serialisation/xml/xml-config/content-model-xml-config.json").openStream()) {
            xmlMapper = RosettaObjectMapperCreator.forXML(inputStream).create();
        }
    }

    // -------------- Example 1: TradeIdentifier tradeId routing --------------

    @Test
    public void testTradeIdentifierVirtualBranch() throws IOException {
        String xml = "<FpmlTradeIdentifier id=\"ti-1\">"
                + "<partyReference href=\"party-1\"/>"
                + "<tradeId scheme=\"urn:trade-id\">ABC-123</tradeId>"
                + "</FpmlTradeIdentifier>";

        FpmlTradeIdentifier actual = xmlMapper.readValue(xml, FpmlTradeIdentifier.class);

        assertEquals("ti-1", actual.getId());
        assertNotNull(actual.getPartyReference());
        assertEquals("party-1", actual.getPartyReference().getHref());
        assertNull(actual.getTradeId());
        assertNotNull(actual.getTradeIdentifierChoice());
        assertEquals(1, actual.getTradeIdentifierChoice().size());
        assertEquals("ABC-123", actual.getTradeIdentifierChoice().get(0).getTradeId().getValue());
        assertEquals("urn:trade-id", actual.getTradeIdentifierChoice().get(0).getTradeId().getScheme());
    }

    @Test
    public void testTradeIdentifierDirectBranch() throws IOException {
        String xml = "<FpmlTradeIdentifier id=\"ti-2\">"
                + "<issuer scheme=\"urn:issuer\">BANK-A</issuer>"
                + "<tradeId scheme=\"urn:trade-id\">ABC-123</tradeId>"
                + "</FpmlTradeIdentifier>";

        FpmlTradeIdentifier actual = xmlMapper.readValue(xml, FpmlTradeIdentifier.class);

        assertEquals("ti-2", actual.getId());
        assertNotNull(actual.getTradeId());
        assertEquals("ABC-123", actual.getTradeId().getValue());
        assertEquals("urn:trade-id", actual.getTradeId().getScheme());
        assertEquals("BANK-A", actual.getIssuer().getValue());
        assertEquals("urn:issuer", actual.getIssuer().getScheme());
        assertTrue(actual.getTradeIdentifierChoice() == null || actual.getTradeIdentifierChoice().isEmpty());
    }

    // -------------- Example 2: FxTargetKnockoutForward constantPayoffRegion --------------

    @Test
    public void testFxConstantPayoffRegionRouting() throws IOException {
        String xml = "<FpmlFxTargetKnockoutForward>"
                + "<constantPayoffRegion id=\"base-constant\"/>"
                + "<linearPayoffRegion id=\"base-linear\"/>"
                + "<constantPayoffRegion id=\"extra-constant\"/>"
                + "<barrier id=\"barrier-1\"/>"
                + "</FpmlFxTargetKnockoutForward>";

        FpmlFxTargetKnockoutForward actual = xmlMapper.readValue(xml, FpmlFxTargetKnockoutForward.class);

        assertNotNull(actual.getConstantPayoffRegion());
        assertEquals(1, actual.getConstantPayoffRegion().size());
        assertEquals("base-constant", actual.getConstantPayoffRegion().get(0).getId());

        assertNotNull(actual.getLinearPayoffRegion());
        assertEquals("base-linear", actual.getLinearPayoffRegion().getId());

        assertNotNull(actual.getFxTargetKnockoutForwardChoice());
        assertEquals(1, actual.getFxTargetKnockoutForwardChoice().size());
        assertEquals("extra-constant",
                actual.getFxTargetKnockoutForwardChoice().get(0).getConstantPayoffRegion().getId());
        assertNull(actual.getFxTargetKnockoutForwardChoice().get(0).getLinearPayoffRegion());

        assertNotNull(actual.getBarrier());
        assertEquals(1, actual.getBarrier().size());
        assertEquals("barrier-1", actual.getBarrier().get(0).getId());
    }

    // -------------- Example 3: FxTargetKnockoutForward linearPayoffRegion --------------

    @Test
    public void testFxLinearPayoffRegionRouting() throws IOException {
        String xml = "<FpmlFxTargetKnockoutForward>"
                + "<constantPayoffRegion id=\"base-constant-1\"/>"
                + "<constantPayoffRegion id=\"base-constant-2\"/>"
                + "<linearPayoffRegion id=\"base-linear\"/>"
                + "<linearPayoffRegion id=\"extra-linear-1\"/>"
                + "<constantPayoffRegion id=\"extra-constant\"/>"
                + "<linearPayoffRegion id=\"extra-linear-2\"/>"
                + "</FpmlFxTargetKnockoutForward>";

        FpmlFxTargetKnockoutForward actual = xmlMapper.readValue(xml, FpmlFxTargetKnockoutForward.class);

        assertEquals(2, actual.getConstantPayoffRegion().size());
        assertEquals("base-constant-1", actual.getConstantPayoffRegion().get(0).getId());
        assertEquals("base-constant-2", actual.getConstantPayoffRegion().get(1).getId());

        assertEquals("base-linear", actual.getLinearPayoffRegion().getId());

        assertEquals(3, actual.getFxTargetKnockoutForwardChoice().size());
        assertEquals("extra-linear-1",
                actual.getFxTargetKnockoutForwardChoice().get(0).getLinearPayoffRegion().getId());
        assertNull(actual.getFxTargetKnockoutForwardChoice().get(0).getConstantPayoffRegion());
        assertEquals("extra-constant",
                actual.getFxTargetKnockoutForwardChoice().get(1).getConstantPayoffRegion().getId());
        assertNull(actual.getFxTargetKnockoutForwardChoice().get(1).getLinearPayoffRegion());
        assertEquals("extra-linear-2",
                actual.getFxTargetKnockoutForwardChoice().get(2).getLinearPayoffRegion().getId());
        assertNull(actual.getFxTargetKnockoutForwardChoice().get(2).getConstantPayoffRegion());
    }

    // -------------- Example 4: Multi-Leaf Virtual Occurrence --------------

    @Test
    public void testMultiLeafVirtualOccurrence() throws IOException {
        String xml = "<MultiLeafContainer>"
                + "<firstValue>A1</firstValue>"
                + "<secondValue>A2</secondValue>"
                + "<firstValue>B1</firstValue>"
                + "<secondValue>B2</secondValue>"
                + "</MultiLeafContainer>";

        MultiLeafContainer actual = xmlMapper.readValue(xml, MultiLeafContainer.class);

        assertNotNull(actual.getEntry());
        assertEquals(2, actual.getEntry().size());
        assertEquals("A1", actual.getEntry().get(0).getFirstValue().getValue());
        assertEquals("A2", actual.getEntry().get(0).getSecondValue().getValue());
        assertEquals("B1", actual.getEntry().get(1).getFirstValue().getValue());
        assertEquals("B2", actual.getEntry().get(1).getSecondValue().getValue());
    }

    // -------------- Additional node kinds: ALL and ANY --------------

    @Test
    public void testAllRoutesUnorderedFieldsToSameVirtualOccurrence() throws IOException {
        String xml = "<AllVirtualContainer>"
                + "<secondValue>A2</secondValue>"
                + "<firstValue>A1</firstValue>"
                + "</AllVirtualContainer>";

        AllVirtualContainer actual = xmlMapper.readValue(xml, AllVirtualContainer.class);

        assertNotNull(actual.getEntry());
        assertEquals(1, actual.getEntry().size());
        assertEquals("A1", actual.getEntry().get(0).getFirstValue().getValue());
        assertEquals("A2", actual.getEntry().get(0).getSecondValue().getValue());
    }

    @Test
    public void testAnyRoutesWildcardFieldToVirtualPath() throws IOException {
        String xml = "<AnyVirtualContainer>"
                + "<unexpectedValue>WILD</unexpectedValue>"
                + "<knownValue>KNOWN</knownValue>"
                + "</AnyVirtualContainer>";

        AnyVirtualContainer actual = xmlMapper.readValue(xml, AnyVirtualContainer.class);

        assertNotNull(actual.getKnownValue());
        assertEquals("KNOWN", actual.getKnownValue().getValue());
        assertNotNull(actual.getEntry());
        assertEquals(1, actual.getEntry().size());
        assertEquals("WILD", actual.getEntry().get(0).getWildcardValue().getValue());
    }

    @Test
    public void testMultiLayerVirtualPathRouting() throws IOException {
        String xml = "<MultiLayerContainer>"
                + "<firstValue>A1</firstValue>"
                + "<secondValue>A2</secondValue>"
                + "<firstValue>B1</firstValue>"
                + "<secondValue>B2</secondValue>"
                + "</MultiLayerContainer>";

        MultiLayerContainer actual = xmlMapper.readValue(xml, MultiLayerContainer.class);

        assertNotNull(actual.getOuter());
        assertEquals(2, actual.getOuter().size());
        assertNotNull(actual.getOuter().get(0).getInner());
        assertEquals(1, actual.getOuter().get(0).getInner().size());
        assertEquals("A1", actual.getOuter().get(0).getInner().get(0).getFirstValue().getValue());
        assertEquals("A2", actual.getOuter().get(0).getInner().get(0).getSecondValue().getValue());
        assertNotNull(actual.getOuter().get(1).getInner());
        assertEquals(1, actual.getOuter().get(1).getInner().size());
        assertEquals("B1", actual.getOuter().get(1).getInner().get(0).getFirstValue().getValue());
        assertEquals("B2", actual.getOuter().get(1).getInner().get(0).getSecondValue().getValue());
    }

    // -------------- Failure cases --------------

    @Test
    public void testFxMissingRequiredLinearPayoffRegionFails() {
        String xml = "<FpmlFxTargetKnockoutForward>"
                + "<constantPayoffRegion id=\"base-constant\"/>"
                + "</FpmlFxTargetKnockoutForward>";

        JsonMappingException ex = assertThrows(JsonMappingException.class,
                () -> xmlMapper.readValue(xml, FpmlFxTargetKnockoutForward.class));
        assertTrue(ex.getMessage().contains("FpmlFxTargetKnockoutForward"),
                "Expected target type name in message: " + ex.getMessage());
    }

    /**
     * Mixed nested choices: a single {@code tradeId} followed by a single {@code versionedTradeId}
     * within the repeated {@code tradeIdentifierChoice} group must produce two separate virtual
     * objects, one per content-model occurrence.
     */
    @Test
    public void testTradeIdentifierMixedNestedChoices() throws IOException {
        String xml = "<FpmlTradeIdentifier>"
                + "<partyReference href=\"party-1\"/>"
                + "<tradeId scheme=\"urn:trade-id\">T-1</tradeId>"
                + "<versionedTradeId scheme=\"urn:version\">V-2</versionedTradeId>"
                + "</FpmlTradeIdentifier>";

        FpmlTradeIdentifier actual = xmlMapper.readValue(xml, FpmlTradeIdentifier.class);

        assertNotNull(actual.getTradeIdentifierChoice());
        assertEquals(2, actual.getTradeIdentifierChoice().size());
        assertEquals("T-1", actual.getTradeIdentifierChoice().get(0).getTradeId().getValue());
        assertNull(actual.getTradeIdentifierChoice().get(0).getVersionedTradeId());
        assertNull(actual.getTradeIdentifierChoice().get(1).getTradeId());
        assertEquals("V-2", actual.getTradeIdentifierChoice().get(1).getVersionedTradeId().getValue());
    }

    @Test
    public void testTradeIdentifierAmbiguousFails() {
        String xml = "<FpmlTradeIdentifier id=\"ti-ambiguous\">"
                + "<issuer scheme=\"urn:issuer\">BANK-A</issuer>"
                + "<partyReference href=\"party-1\"/>"
                + "<tradeId scheme=\"urn:trade-id\">ABC-123</tradeId>"
                + "</FpmlTradeIdentifier>";

        JsonMappingException ex = assertThrows(JsonMappingException.class,
                () -> xmlMapper.readValue(xml, FpmlTradeIdentifier.class));
        assertTrue(ex.getMessage().contains("FpmlTradeIdentifier"),
                "Expected target type name in message: " + ex.getMessage());
    }

    // -------------- Config loading tests --------------

    @Test
    public void testConfigLoadingNodeTypeAndChildren() throws IOException {
        RosettaXMLConfiguration cfg = loadConfig();
        ModelSymbolId symbol = new ModelSymbolId(
                DottedPath.splitOnDots("com.rosetta.test"), "FpmlTradeIdentifier");
        Optional<TypeXMLConfiguration> tcfg = cfg.getConfigurationForType(symbol);
        assertTrue(tcfg.isPresent());
        Optional<XMLContentModel> root = tcfg.get().getContentModel();
        assertTrue(root.isPresent(), "contentModel expected to be present");
        assertEquals(XMLContentModelNodeType.CHOICE, root.get().getNodeType());
        assertTrue(root.get().getChildren().isPresent());
        List<XMLContentModel> branches = root.get().getChildren().get();
        assertEquals(2, branches.size());
        assertEquals(XMLContentModelNodeType.SEQUENCE, branches.get(0).getNodeType());
        assertEquals(XMLContentModelNodeType.SEQUENCE, branches.get(1).getNodeType());
    }

    @Test
    public void testConfigLoadingMaxOccursUnboundedAndNumeric() throws IOException {
        RosettaXMLConfiguration cfg = loadConfig();
        ModelSymbolId symbol = new ModelSymbolId(
                DottedPath.splitOnDots("com.rosetta.test"), "FpmlFxTargetKnockoutForward");
        Optional<TypeXMLConfiguration> tcfg = cfg.getConfigurationForType(symbol);
        assertTrue(tcfg.isPresent());
        XMLContentModel root = tcfg.get().getContentModel().get();
        List<XMLContentModel> children = root.getChildren().get();
        // pivot: maxOccurs=1 (numeric)
        XMLContentModel pivot = children.get(0);
        assertEquals("pivot", pivot.getXmlName().orElse(null));
        assertEquals(Integer.valueOf(0), pivot.getMinOccurs().get());
        assertTrue(pivot.getMaxOccurs().isPresent());
        assertEquals(OccursMax.of(1), pivot.getMaxOccurs().get());
        // constantPayoffRegion: maxOccurs="unbounded"
        XMLContentModel constantPayoffRegion = children.get(1);
        assertEquals("constantPayoffRegion", constantPayoffRegion.getXmlName().orElse(null));
        assertTrue(constantPayoffRegion.getMaxOccurs().isPresent());
        assertTrue(constantPayoffRegion.getMaxOccurs().get().isUnbounded());
        // last child is the CHOICE with maxOccurs=unbounded
        XMLContentModel choice = children.get(3);
        assertEquals(XMLContentModelNodeType.CHOICE, choice.getNodeType());
        assertTrue(choice.getMaxOccurs().get().isUnbounded());
    }

    private static RosettaXMLConfiguration loadConfig() throws IOException {
        try (InputStream inputStream = Resources.getResource(
                "serialisation/xml/xml-config/content-model-xml-config.json").openStream()) {
            return RosettaXMLConfiguration.load(inputStream);
        }
    }
}
