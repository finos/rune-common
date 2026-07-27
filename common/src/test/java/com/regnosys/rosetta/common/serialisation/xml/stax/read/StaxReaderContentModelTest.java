package com.regnosys.rosetta.common.serialisation.xml.stax.read;

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

import com.google.common.io.Resources;
import com.regnosys.rosetta.common.serialisation.xml.config.RosettaXMLConfiguration;
import com.rosetta.test.AllVirtualContainer;
import com.rosetta.test.AnyVirtualContainer;
import com.rosetta.test.FpmlFxTargetKnockoutForward;
import com.rosetta.test.FpmlTradeIdentifier;
import com.rosetta.test.MultiLayerContainer;
import com.rosetta.test.MultiLeafContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Content-model disambiguation on read (Step 4c of the StAX migration plan).
 *
 * <p>The first group of tests mirrors {@code XmlContentModelDisambiguationTest} case for case, so
 * the StAX binder is held to exactly the Jackson engine's routing behaviour — the four worked
 * examples, ALL/ANY, multi-layer virtual paths, and the lenient-recovery cases.</p>
 *
 * <p>The namespace group has no Jackson counterpart: those cases only pass because the StAX reader
 * feeds the matcher real namespace state. The Jackson path buffers each element into a
 * {@code TokenBuffer}, which discards the StAX namespace context, so it was forced into the
 * matcher's permissive {@code UNKNOWN} fallback (issue 6).</p>
 */
public class StaxReaderContentModelTest {

    private static final String XML_CONFIG =
            "serialisation/xml/xml-config/content-model-xml-config.json";
    private static final String NAMESPACE_XML_CONFIG =
            "serialisation/xml/xml-config/content-model-namespace-xml-config.json";

    private StaxReader reader;

    @BeforeEach
    public void setUp() throws Exception {
        reader = readerFor(XML_CONFIG);
    }

    private StaxReader readerFor(String configResource) throws Exception {
        URL configUrl = Resources.getResource(configResource);
        RosettaXMLConfiguration config;
        try (InputStream is = configUrl.openStream()) {
            config = RosettaXMLConfiguration.load(is);
        }
        return new StaxReader(config, getClass().getClassLoader());
    }

    // -------------- Example 1: TradeIdentifier tradeId routing --------------

    @Test
    public void testTradeIdentifierVirtualBranch() throws Exception {
        String xml = "<FpmlTradeIdentifier id=\"ti-1\">"
                + "<partyReference href=\"party-1\"/>"
                + "<tradeId scheme=\"urn:trade-id\">ABC-123</tradeId>"
                + "</FpmlTradeIdentifier>";

        FpmlTradeIdentifier actual = reader.read(xml, FpmlTradeIdentifier.class);

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
    public void testTradeIdentifierDirectBranch() throws Exception {
        String xml = "<FpmlTradeIdentifier id=\"ti-2\">"
                + "<issuer scheme=\"urn:issuer\">BANK-A</issuer>"
                + "<tradeId scheme=\"urn:trade-id\">ABC-123</tradeId>"
                + "</FpmlTradeIdentifier>";

        FpmlTradeIdentifier actual = reader.read(xml, FpmlTradeIdentifier.class);

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
    public void testFxConstantPayoffRegionRouting() throws Exception {
        String xml = "<FpmlFxTargetKnockoutForward>"
                + "<constantPayoffRegion id=\"base-constant\"/>"
                + "<linearPayoffRegion id=\"base-linear\"/>"
                + "<constantPayoffRegion id=\"extra-constant\"/>"
                + "<barrier id=\"barrier-1\"/>"
                + "</FpmlFxTargetKnockoutForward>";

        FpmlFxTargetKnockoutForward actual = reader.read(xml, FpmlFxTargetKnockoutForward.class);

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

        // 'barrier' is absent from the content model, so it is bound by plain name matching.
        assertNotNull(actual.getBarrier());
        assertEquals(1, actual.getBarrier().size());
        assertEquals("barrier-1", actual.getBarrier().get(0).getId());
    }

    // -------------- Example 3: FxTargetKnockoutForward linearPayoffRegion --------------

    @Test
    public void testFxLinearPayoffRegionRouting() throws Exception {
        String xml = "<FpmlFxTargetKnockoutForward>"
                + "<constantPayoffRegion id=\"base-constant-1\"/>"
                + "<constantPayoffRegion id=\"base-constant-2\"/>"
                + "<linearPayoffRegion id=\"base-linear\"/>"
                + "<linearPayoffRegion id=\"extra-linear-1\"/>"
                + "<constantPayoffRegion id=\"extra-constant\"/>"
                + "<linearPayoffRegion id=\"extra-linear-2\"/>"
                + "</FpmlFxTargetKnockoutForward>";

        FpmlFxTargetKnockoutForward actual = reader.read(xml, FpmlFxTargetKnockoutForward.class);

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
    public void testMultiLeafVirtualOccurrence() throws Exception {
        String xml = "<MultiLeafContainer>"
                + "<firstValue>A1</firstValue>"
                + "<secondValue>A2</secondValue>"
                + "<firstValue>B1</firstValue>"
                + "<secondValue>B2</secondValue>"
                + "</MultiLeafContainer>";

        MultiLeafContainer actual = reader.read(xml, MultiLeafContainer.class);

        assertNotNull(actual.getEntry());
        assertEquals(2, actual.getEntry().size());
        assertEquals("A1", actual.getEntry().get(0).getFirstValue().getValue());
        assertEquals("A2", actual.getEntry().get(0).getSecondValue().getValue());
        assertEquals("B1", actual.getEntry().get(1).getFirstValue().getValue());
        assertEquals("B2", actual.getEntry().get(1).getSecondValue().getValue());
    }

    // -------------- Additional node kinds: ALL and ANY --------------

    @Test
    public void testAllRoutesUnorderedFieldsToSameVirtualOccurrence() throws Exception {
        String xml = "<AllVirtualContainer>"
                + "<secondValue>A2</secondValue>"
                + "<firstValue>A1</firstValue>"
                + "</AllVirtualContainer>";

        AllVirtualContainer actual = reader.read(xml, AllVirtualContainer.class);

        assertNotNull(actual.getEntry());
        assertEquals(1, actual.getEntry().size());
        assertEquals("A1", actual.getEntry().get(0).getFirstValue().getValue());
        assertEquals("A2", actual.getEntry().get(0).getSecondValue().getValue());
    }

    @Test
    public void testAnyRoutesWildcardFieldToVirtualPath() throws Exception {
        String xml = "<AnyVirtualContainer>"
                + "<unexpectedValue>WILD</unexpectedValue>"
                + "<knownValue>KNOWN</knownValue>"
                + "</AnyVirtualContainer>";

        AnyVirtualContainer actual = reader.read(xml, AnyVirtualContainer.class);

        assertNotNull(actual.getKnownValue());
        assertEquals("KNOWN", actual.getKnownValue().getValue());
        assertNotNull(actual.getEntry());
        assertEquals(1, actual.getEntry().size());
        assertEquals("WILD", actual.getEntry().get(0).getWildcardValue().getValue());
    }

    @Test
    public void testMultiLayerVirtualPathRouting() throws Exception {
        String xml = "<MultiLayerContainer>"
                + "<firstValue>A1</firstValue>"
                + "<secondValue>A2</secondValue>"
                + "<firstValue>B1</firstValue>"
                + "<secondValue>B2</secondValue>"
                + "</MultiLayerContainer>";

        MultiLayerContainer actual = reader.read(xml, MultiLayerContainer.class);

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

    /**
     * Mixed nested choices: a single {@code tradeId} followed by a single {@code versionedTradeId}
     * within the repeated {@code tradeIdentifierChoice} group must produce two separate virtual
     * objects, one per content-model occurrence.
     */
    @Test
    public void testTradeIdentifierMixedNestedChoices() throws Exception {
        String xml = "<FpmlTradeIdentifier>"
                + "<partyReference href=\"party-1\"/>"
                + "<tradeId scheme=\"urn:trade-id\">T-1</tradeId>"
                + "<versionedTradeId scheme=\"urn:version\">V-2</versionedTradeId>"
                + "</FpmlTradeIdentifier>";

        FpmlTradeIdentifier actual = reader.read(xml, FpmlTradeIdentifier.class);

        assertNotNull(actual.getTradeIdentifierChoice());
        assertEquals(2, actual.getTradeIdentifierChoice().size());
        assertEquals("T-1", actual.getTradeIdentifierChoice().get(0).getTradeId().getValue());
        assertNull(actual.getTradeIdentifierChoice().get(0).getVersionedTradeId());
        assertNull(actual.getTradeIdentifierChoice().get(1).getTradeId());
        assertEquals("V-2", actual.getTradeIdentifierChoice().get(1).getVersionedTradeId().getValue());
    }

    // -------------- Lenient-recovery cases --------------

    @Test
    public void testFxMissingRequiredLinearPayoffRegionIsLenient() throws Exception {
        // The content model requires linearPayoffRegion, so nothing matches strictly. Rather than
        // rejecting the document, the reader keeps the elements it can place by name.
        String xml = "<FpmlFxTargetKnockoutForward>"
                + "<constantPayoffRegion id=\"base-constant\"/>"
                + "</FpmlFxTargetKnockoutForward>";

        FpmlFxTargetKnockoutForward actual = reader.read(xml, FpmlFxTargetKnockoutForward.class);

        assertNotNull(actual.getConstantPayoffRegion());
        assertEquals(1, actual.getConstantPayoffRegion().size());
        assertEquals("base-constant", actual.getConstantPayoffRegion().get(0).getId());
    }

    @Test
    public void testTradeIdentifierAmbiguousIsLenient() throws Exception {
        // Genuinely ambiguous against the content model (issuer belongs to one branch,
        // partyReference to another). No exception: what can be placed by name is kept.
        String xml = "<FpmlTradeIdentifier id=\"ti-ambiguous\">"
                + "<issuer scheme=\"urn:issuer\">BANK-A</issuer>"
                + "<partyReference href=\"party-1\"/>"
                + "<tradeId scheme=\"urn:trade-id\">ABC-123</tradeId>"
                + "</FpmlTradeIdentifier>";

        FpmlTradeIdentifier actual = reader.read(xml, FpmlTradeIdentifier.class);

        assertEquals("ti-ambiguous", actual.getId());
        assertEquals("BANK-A", actual.getIssuer().getValue());
        assertNotNull(actual.getPartyReference());
        assertEquals("party-1", actual.getPartyReference().getHref());
    }

    @Test
    public void testMisorderedInputIsReorderedAndDeserialised() throws Exception {
        // Safety net: an XML document whose elements are valid but out of schema order
        // (versionedTradeId before partyReference) must still deserialise, by being stably
        // reordered into content-model order rather than rejected.
        String xml = "<FpmlTradeIdentifier>"
                + "<versionedTradeId scheme=\"urn:version\">V-1</versionedTradeId>"
                + "<partyReference href=\"party-1\"/>"
                + "</FpmlTradeIdentifier>";

        FpmlTradeIdentifier actual = reader.read(xml, FpmlTradeIdentifier.class);

        assertNotNull(actual.getPartyReference());
        assertEquals("party-1", actual.getPartyReference().getHref());
        assertNotNull(actual.getTradeIdentifierChoice());
        assertEquals(1, actual.getTradeIdentifierChoice().size());
        assertEquals("V-1", actual.getTradeIdentifierChoice().get(0).getVersionedTradeId().getValue());
    }

    // -------------- Namespace-aware routing (no Jackson equivalent) --------------

    /**
     * A namespace-qualified content model matches elements in the required namespace and groups them
     * by occurrence exactly as the unqualified model does.
     */
    @Test
    public void testNamespaceQualifiedContentModelRoutes() throws Exception {
        StaxReader nsReader = readerFor(NAMESPACE_XML_CONFIG);
        String xml = "<MultiLeafContainer xmlns=\"urn:my.schema\">"
                + "<firstValue>A1</firstValue>"
                + "<secondValue>A2</secondValue>"
                + "<firstValue>B1</firstValue>"
                + "<secondValue>B2</secondValue>"
                + "</MultiLeafContainer>";

        MultiLeafContainer actual = nsReader.read(xml, MultiLeafContainer.class);

        assertEquals(2, actual.getEntry().size());
        assertEquals("A1", actual.getEntry().get(0).getFirstValue().getValue());
        assertEquals("A2", actual.getEntry().get(0).getSecondValue().getValue());
        assertEquals("B1", actual.getEntry().get(1).getFirstValue().getValue());
        assertEquals("B2", actual.getEntry().get(1).getSecondValue().getValue());
    }

    /**
     * The same document in the wrong namespace is <em>not</em> routed by local name alone: the
     * content model requires {@code urn:my.schema}, the elements are in another namespace, so
     * routing correctly finds no match and the elements fall back to plain name binding
     * ({@link StaxReader#handleChildElement}) instead of the router/{@link VirtualPathAssembler}
     * path. Since Step 4d, that fallback path also accumulates every occurrence of a repeated
     * unwrapped group (issue 7 / criterion 17) rather than collapsing to the last one, so the two
     * occurrences still round-trip correctly even without content-model routing.
     *
     * <p>Jackson could not reach this behaviour: {@code TokenBuffer} replay reports the namespace as
     * {@code UNKNOWN}, which the matcher treats permissively, so a foreign-namespace element matched
     * as if it belonged (issue 6).</p>
     */
    @Test
    public void testWrongNamespaceIsNotRoutedByLocalNameAlone() throws Exception {
        StaxReader nsReader = readerFor(NAMESPACE_XML_CONFIG);
        String xml = "<MultiLeafContainer xmlns=\"urn:some.other.schema\">"
                + "<firstValue>A1</firstValue>"
                + "<secondValue>A2</secondValue>"
                + "<firstValue>B1</firstValue>"
                + "<secondValue>B2</secondValue>"
                + "</MultiLeafContainer>";

        MultiLeafContainer actual = nsReader.read(xml, MultiLeafContainer.class);

        // The content model rejects routing (wrong namespace), so this falls back to plain
        // name-based binding (StaxReader#handleChildElement) rather than the router/
        // VirtualPathAssembler path. That fallback is the repeated-unwrapped-group path fixed by
        // Step 4d (issue 7 / criterion 17): it must still accumulate both occurrences, not
        // collapse to the last one.
        assertEquals(2, actual.getEntry().size());
        assertEquals("A1", actual.getEntry().get(0).getFirstValue().getValue());
        assertEquals("A2", actual.getEntry().get(0).getSecondValue().getValue());
        assertEquals("B1", actual.getEntry().get(1).getFirstValue().getValue());
        assertEquals("B2", actual.getEntry().get(1).getSecondValue().getValue());
    }

    /**
     * Two content-model branches share the local name {@code value} and differ only by namespace.
     * The StAX reader routes each element to its own slot by namespace; the Jackson path reports this
     * as ambiguous (see {@code XMLContentModelMatcherNamespaceTest}) and loses the distinction.
     */
    @Test
    public void testSameLocalNameInDifferentNamespacesRoutesToDistinctSlots() throws Exception {
        StaxReader nsReader = readerFor(NAMESPACE_XML_CONFIG);
        String xml = "<AllVirtualContainer xmlns=\"urn:my.schema\" xmlns:ext=\"urn:my.extension\">"
                + "<value>FROM-SCHEMA</value>"
                + "<ext:value>FROM-EXTENSION</ext:value>"
                + "</AllVirtualContainer>";

        AllVirtualContainer actual = nsReader.read(xml, AllVirtualContainer.class);

        assertEquals(2, actual.getEntry().size());
        assertEquals("FROM-SCHEMA", actual.getEntry().get(0).getFirstValue().getValue());
        assertNull(actual.getEntry().get(0).getSecondValue());
        assertNull(actual.getEntry().get(1).getFirstValue());
        assertEquals("FROM-EXTENSION", actual.getEntry().get(1).getSecondValue().getValue());
    }
}
