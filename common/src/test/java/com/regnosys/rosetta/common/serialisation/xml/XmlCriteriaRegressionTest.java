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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapperCreator;
import com.rosetta.regression.AssetTransfer;
import com.rosetta.regression.BaseCommodityOption;
import com.rosetta.regression.CommoditySwap;
import com.rosetta.regression.ExtensionCommodityOption;
import com.rosetta.regression.ExtensionPhysicalLeg;
import com.rosetta.regression.LegSchedule;
import com.rosetta.regression.ReferenceEntityAsset;
import com.rosetta.regression.ScheduleGroup;
import com.rosetta.regression.TradeIdentifier;
import com.rosetta.regression.TradeUnderlyer;
import com.rosetta.regression.TransactionLeg;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for acceptance criteria 13-17 of the StAX binder migration (Section 1).
 *
 * <p>Each criterion corresponds to one of the catalogued Jackson XML defects, and each of those
 * defects had a "pick the first" workaround that silently dropped data. The fixtures are anonymous
 * stand-ins defined in {@code rosetta/rosetta-regression-type.rosetta}, keeping the exact XML shape
 * that broke while depending on nothing outside this repository. Every test asserts that the data
 * which used to be lost is now present — not merely that parsing succeeds.
 *
 * <p>All tests go through {@link RosettaObjectMapperCreator#forXML} so the binder is exercised via
 * the unchanged public entry point, and each round-trips the object back to XML to prove the write
 * side preserves the distinctions the read side made.
 */
public class XmlCriteriaRegressionTest {

    private final ObjectMapper xmlMapper;

    public XmlCriteriaRegressionTest() throws IOException {
        try (InputStream inputStream = Resources.getResource(
                "serialisation/xml/xml-config/regression-xml-config.json").openStream()) {
            xmlMapper = RosettaObjectMapperCreator.forXML(inputStream).create();
        }
    }

    // -------------------------------------------------------------------------
    // Criterion 13 / issue 1 — same local name as XML attribute and child element
    // -------------------------------------------------------------------------

    /**
     * A type carrying attribute {@code id} and element {@code id}. Jackson indexed properties by
     * local name alone, so the two collided and one clobbered the other; StAX reads attributes and
     * child elements through entirely separate APIs, making the collision structurally impossible.
     */
    @Test
    public void criterion13_attributeAndElementIdBothSurviveMultiCardinality() throws IOException {
        String xml = "<TransactionLeg id=\"ATTR-ID\">"
                + "<id legIdScheme=\"scheme-1\">ELEM-ID-1</id>"
                + "<id legIdScheme=\"scheme-2\">ELEM-ID-2</id>"
                + "<buyerPartyReference>party-1</buyerPartyReference>"
                + "</TransactionLeg>";

        TransactionLeg actual = xmlMapper.readValue(xml, TransactionLeg.class);

        // The attribute survives...
        assertEquals("ATTR-ID", actual.getMetaId());
        // ...and so does every element, with its own attributes intact.
        assertEquals(2, actual.getId().size());
        assertEquals("ELEM-ID-1", actual.getId().get(0).getValue());
        assertEquals("scheme-1", actual.getId().get(0).getLegIdScheme());
        assertEquals("ELEM-ID-2", actual.getId().get(1).getValue());
        assertEquals("scheme-2", actual.getId().get(1).getLegIdScheme());
        assertEquals("party-1", actual.getBuyerPartyReference());

        assertRoundTrips(actual, TransactionLeg.class);
    }

    /** The same collision where the colliding element is single-cardinality. */
    @Test
    public void criterion13_attributeAndElementIdBothSurviveSingleCardinality() throws IOException {
        String xml = "<AssetTransfer id=\"ATTR-ID\">"
                + "<id>ELEM-ID</id>"
                + "<transferAmount>1000</transferAmount>"
                + "</AssetTransfer>";

        AssetTransfer actual = xmlMapper.readValue(xml, AssetTransfer.class);

        assertEquals("ATTR-ID", actual.getMetaId());
        assertNotNull(actual.getId(), "the child element <id> must not be clobbered by attribute id");
        assertEquals("ELEM-ID", actual.getId().getValue());
        assertEquals("1000", actual.getTransferAmount());

        assertRoundTrips(actual, AssetTransfer.class);
    }

    // -------------------------------------------------------------------------
    // Criterion 14 / issue 3 (routing half) — one element name across unwrapped layers
    // -------------------------------------------------------------------------

    /**
     * {@code tradeId} exists both directly and inside the unwrapped choice group. Jackson
     * deserialised the element into both layers; the content model must route it to exactly one
     * slot — here the choice group, because {@code partyReference} selects that branch.
     */
    @Test
    public void criterion14_tradeIdRoutedToExactlyOneSlot() throws IOException {
        String xml = "<TradeIdentifier>"
                + "<partyReference href=\"party-1\"/>"
                + "<tradeId>TID-1</tradeId>"
                + "</TradeIdentifier>";

        TradeIdentifier actual = xmlMapper.readValue(xml, TradeIdentifier.class);

        assertEquals("party-1", actual.getPartyReference().getHref());
        assertEquals(1, actual.getTradeIdentifierChoice().size());
        assertEquals("TID-1", actual.getTradeIdentifierChoice().get(0).getTradeId().getValue());
        assertNull(actual.getTradeId(),
                "tradeId must be routed to the choice group only, not deserialised into both layers");

        assertRoundTrips(actual, TradeIdentifier.class);
    }

    /**
     * The other branch of the same content model: with no {@code partyReference}, {@code tradeId}
     * routes to the <em>direct</em> property instead. Same element name, different slot — precisely
     * what a property map keyed by local name cannot express.
     */
    @Test
    public void criterion14_tradeIdRoutesToDirectSlotOnTheOtherBranch() throws IOException {
        String xml = "<TradeIdentifier><tradeId>TID-2</tradeId></TradeIdentifier>";

        TradeIdentifier actual = xmlMapper.readValue(xml, TradeIdentifier.class);

        assertEquals("TID-2", actual.getTradeId().getValue());
        // Pruning turns an unpopulated list into null, so "empty" means null-or-empty here.
        assertTrue(actual.getTradeIdentifierChoice() == null || actual.getTradeIdentifierChoice().isEmpty(),
                "the choice group must stay empty on this branch");
    }

    // -------------------------------------------------------------------------
    // Criterion 15 / issue 5 — substituted name collides with a direct element
    // -------------------------------------------------------------------------

    /**
     * A direct element {@code referenceEntity} alongside an {@code underlyingAsset} substitution
     * member whose substituted element name is also {@code referenceEntity}. Jackson kept one of
     * them, leaving the direct property never populated; content-model position separates the two
     * occurrences into distinct slots.
     */
    @Test
    public void criterion15_substitutedNameCollidingWithDirectElementPopulatesBothSlots() throws IOException {
        String xml = "<TradeUnderlyer>"
                + "<referenceEntity entityId=\"LEI-1\"/>"
                + "<referenceEntity><entityName>Acme Corp</entityName></referenceEntity>"
                + "</TradeUnderlyer>";

        TradeUnderlyer actual = xmlMapper.readValue(xml, TradeUnderlyer.class);

        // The direct property — the one the Jackson workaround left permanently null.
        assertNotNull(actual.getReferenceEntity(),
                "the direct referenceEntity property must be populated, not dropped");
        assertEquals("LEI-1", actual.getReferenceEntity().getEntityId());

        // ...and the substitution-group member resolves to its own concrete type in its own slot.
        assertEquals(1, actual.getUnderlyingAsset().size());
        ReferenceEntityAsset asset =
                assertInstanceOf(ReferenceEntityAsset.class, actual.getUnderlyingAsset().get(0));
        assertEquals("Acme Corp", asset.getEntityName());
    }

    // -------------------------------------------------------------------------
    // Criterion 16 / issue 6 — same local name in different namespaces
    // -------------------------------------------------------------------------

    /**
     * The extension schema substitutes {@code physicalLeg} for the base schema's
     * {@code commoditySwapLeg} — a parse failure under Jackson — and declares its own
     * {@code commodityOption} shadowing the base schema's, which loses the extension-only
     * {@code schedule}. Resolution is by (namespace, local name), so each element binds to the type
     * from its own namespace and {@code schedule} is retained.
     */
    @Test
    public void criterion16_extensionLegsResolveByNamespaceAndRetainSchedule() throws IOException {
        String xml = "<CommoditySwap xmlns:base=\"urn:my.schema\" xmlns:ext=\"urn:my.extension\">"
                + "<ext:physicalLeg>"
                + "<physicalProduct>Allowance</physicalProduct>"
                + "<schedule><startDate>2026-01-01</startDate><endDate>2026-12-31</endDate></schedule>"
                + "</ext:physicalLeg>"
                + "<ext:commodityOption>"
                + "<strikePrice>42</strikePrice>"
                + "<schedule><startDate>2026-02-01</startDate><endDate>2026-11-30</endDate></schedule>"
                + "</ext:commodityOption>"
                + "<base:commodityOption><strikePrice>99</strikePrice></base:commodityOption>"
                + "</CommoditySwap>";

        CommoditySwap actual = xmlMapper.readValue(xml, CommoditySwap.class);
        assertEquals(3, actual.getCommoditySwapLeg().size());

        // 1. The extension-only substitute parses at all, and keeps its schedule.
        ExtensionPhysicalLeg physicalLeg =
                assertInstanceOf(ExtensionPhysicalLeg.class, actual.getCommoditySwapLeg().get(0));
        assertEquals("Allowance", physicalLeg.getPhysicalProduct());
        assertNotNull(physicalLeg.getSchedule(), "the extension-only schedule must be retained");
        assertEquals("2026-01-01", physicalLeg.getSchedule().getScheduleGroup().get(0).getStartDate());

        // 2. <ext:commodityOption> resolves to the extension type, NOT the base-schema type that
        //    shadows it by local name — the "first wins" bug, whose lost property is the schedule.
        ExtensionCommodityOption extOption =
                assertInstanceOf(ExtensionCommodityOption.class, actual.getCommoditySwapLeg().get(1));
        assertEquals("42", extOption.getStrikePrice());
        assertNotNull(extOption.getSchedule(), "the extension commodityOption's schedule must be retained");
        assertEquals("2026-11-30", extOption.getSchedule().getScheduleGroup().get(0).getEndDate());

        // 3. The identically-named base-schema element still resolves to the base-schema type.
        BaseCommodityOption baseOption =
                assertInstanceOf(BaseCommodityOption.class, actual.getCommoditySwapLeg().get(2));
        assertEquals("99", baseOption.getStrikePrice());
    }

    // -------------------------------------------------------------------------
    // Criterion 17 / issue 7 — repeated unwrapped group
    // -------------------------------------------------------------------------

    /**
     * A repeated unwrapped group ({@code <group ref maxOccurs="unbounded">}) must accumulate every
     * occurrence. Jackson's unwrapping extension kept only the first — data loss inside the very
     * extension that had been added to work around an earlier Jackson limitation.
     */
    @Test
    public void criterion17_repeatedUnwrappedGroupAccumulatesAllOccurrences() throws IOException {
        String xml = "<LegSchedule>"
                + "<startDate>2026-01-01</startDate><endDate>2026-03-31</endDate>"
                + "<startDate>2026-04-01</startDate><endDate>2026-06-30</endDate>"
                + "<startDate>2026-07-01</startDate><endDate>2026-09-30</endDate>"
                + "</LegSchedule>";

        LegSchedule actual = xmlMapper.readValue(xml, LegSchedule.class);

        assertEquals(3, actual.getScheduleGroup().size(),
                "every occurrence of the repeated unwrapped group must be accumulated");
        assertScheduleGroup(actual.getScheduleGroup().get(0), "2026-01-01", "2026-03-31");
        assertScheduleGroup(actual.getScheduleGroup().get(1), "2026-04-01", "2026-06-30");
        assertScheduleGroup(actual.getScheduleGroup().get(2), "2026-07-01", "2026-09-30");

        assertRoundTrips(actual, LegSchedule.class);
    }

    private void assertScheduleGroup(ScheduleGroup group, String start, String end) {
        assertEquals(start, group.getStartDate());
        assertEquals(end, group.getEndDate());
    }

    /**
     * Serialises then re-reads the object, asserting the result equals the original. Without this a
     * test could pass on read while the writer silently dropped the recovered data.
     */
    private <T> void assertRoundTrips(T object, Class<T> type) throws IOException {
        String xml = xmlMapper.writeValueAsString(object);
        T reRead = xmlMapper.readValue(xml, type);
        assertEquals(object, reRead, "object must survive a write/read round-trip:\n" + xml);
    }
}
