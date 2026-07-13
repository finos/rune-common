package com.regnosys.rosetta.common.serialisation.xml.spike;

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

// ============================================================
// SPIKE / THROWAWAY CODE — DO NOT PROMOTE TO PRODUCTION
//
// Purpose: prove that the raw javax.xml.stream API (backed by
// Woodstox) can round-trip generated Rune types without Jackson.
// This is Step 0 of the StAX-binder migration spike.
// ============================================================

import com.rosetta.test.Measure;
import com.rosetta.test.TimeContainer;
import com.rosetta.test.UnitEnum;
import org.junit.jupiter.api.Test;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * SPIKE — raw StAX read/write round-trip for generated Rune types.
 *
 * <p>Demonstrates that the {@code javax.xml.stream} API, backed by Woodstox,
 * can:
 * <ul>
 *   <li>write and re-read {@code LocalTime} fields as element text,</li>
 *   <li>write and re-read {@code BigDecimal} + enum fields,</li>
 *   <li>distinguish XML attributes from elements (via
 *       {@link XMLStreamWriter#writeAttribute}/{@link XMLStreamReader#getAttributeValue}).</li>
 * </ul>
 * No introspection or config layer — field names are hard-coded intentionally.
 */
public class StaxBinderSpikeTest {

    // Shared factories — XMLOutputFactory and XMLInputFactory are thread-safe.
    private static final XMLOutputFactory OUTPUT_FACTORY = XMLOutputFactory.newInstance();
    private static final XMLInputFactory INPUT_FACTORY = XMLInputFactory.newInstance();

    // ----------------------------------------------------------------
    // Test 1: TimeContainer round-trip
    // ----------------------------------------------------------------

    @Test
    void timeContainerRoundTrip() throws XMLStreamException {
        LocalTime original = LocalTime.of(1, 23, 45);
        TimeContainer expected = TimeContainer.builder()
                .setTimeValue(original)
                .build();

        String xml = writeTimeContainer(expected);
        TimeContainer actual = readTimeContainer(xml);

        assertEquals(expected, actual,
                "TimeContainer must survive a StAX write→read round-trip");
    }

    private String writeTimeContainer(TimeContainer obj) throws XMLStreamException {
        StringWriter sw = new StringWriter();
        XMLStreamWriter writer = OUTPUT_FACTORY.createXMLStreamWriter(sw);
        writer.writeStartDocument("UTF-8", "1.0");
        writer.writeStartElement("TimeContainer");
        writer.writeStartElement("timeValue");
        writer.writeCharacters(obj.getTimeValue().toString()); // spike: LocalTime.toString()
        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.flush();
        writer.close();
        return sw.toString();
    }

    private TimeContainer readTimeContainer(String xml) throws XMLStreamException {
        TimeContainer.TimeContainerBuilder builder = TimeContainer.builder();
        XMLStreamReader reader = INPUT_FACTORY.createXMLStreamReader(new StringReader(xml));
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT
                    && "timeValue".equals(reader.getLocalName())) {
                String text = reader.getElementText(); // advances past END_ELEMENT
                builder.setTimeValue(LocalTime.parse(text));
            }
        }
        reader.close();
        return builder.build();
    }

    // ----------------------------------------------------------------
    // Test 2: Measure round-trip
    // ----------------------------------------------------------------

    @Test
    void measureRoundTrip() throws XMLStreamException {
        Measure expected = Measure.builder()
                .setValue(new BigDecimal("1.500000000000000000000"))
                .setUnit(UnitEnum.KILOGRAM)
                .build();

        String xml = writeMeasure(expected);
        Measure actual = readMeasure(xml);

        assertEquals(expected, actual,
                "Measure must survive a StAX write→read round-trip");
    }

    private String writeMeasure(Measure obj) throws XMLStreamException {
        StringWriter sw = new StringWriter();
        XMLStreamWriter writer = OUTPUT_FACTORY.createXMLStreamWriter(sw);
        writer.writeStartDocument("UTF-8", "1.0");
        writer.writeStartElement("Measure");
        writer.writeStartElement("value");
        writer.writeCharacters(obj.getValue().toPlainString());
        writer.writeEndElement();
        writer.writeStartElement("unit");
        writer.writeCharacters(obj.getUnit().name()); // spike: enum by name()
        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.flush();
        writer.close();
        return sw.toString();
    }

    private Measure readMeasure(String xml) throws XMLStreamException {
        Measure.MeasureBuilder builder = Measure.builder();
        XMLStreamReader reader = INPUT_FACTORY.createXMLStreamReader(new StringReader(xml));
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String localName = reader.getLocalName();
                if ("value".equals(localName)) {
                    builder.setValue(new BigDecimal(reader.getElementText()));
                } else if ("unit".equals(localName)) {
                    builder.setUnit(UnitEnum.valueOf(reader.getElementText()));
                }
            }
        }
        reader.close();
        return builder.build();
    }

    // ----------------------------------------------------------------
    // Test 3: Attribute distinction — element vs attribute vs text
    // ----------------------------------------------------------------

    @Test
    void attributeDistinction() throws XMLStreamException {
        // Write a root element with a synthetic XML attribute and a child element.
        StringWriter sw = new StringWriter();
        XMLStreamWriter writer = OUTPUT_FACTORY.createXMLStreamWriter(sw);
        writer.writeStartDocument("UTF-8", "1.0");
        writer.writeStartElement("TimeContainer");
        writer.writeAttribute("version", "1.0"); // XML attribute on root
        writer.writeStartElement("timeValue");
        writer.writeCharacters("01:23:45");       // element text content
        writer.writeEndElement();
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.flush();
        writer.close();

        String xml = sw.toString();

        // Read back: verify attribute, element text, and that they are distinct.
        XMLStreamReader reader = INPUT_FACTORY.createXMLStreamReader(new StringReader(xml));
        String capturedVersion = null;
        String capturedTimeValue = null;

        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String localName = reader.getLocalName();
                if ("TimeContainer".equals(localName)) {
                    // Read the XML attribute from the element (not text content)
                    capturedVersion = reader.getAttributeValue(null, "version");
                } else if ("timeValue".equals(localName)) {
                    capturedTimeValue = reader.getElementText();
                }
            }
        }
        reader.close();

        assertEquals("1.0", capturedVersion,
                "XML attribute 'version' must be readable via getAttributeValue()");
        assertEquals("01:23:45", capturedTimeValue,
                "Element text content must be distinct from the attribute");
    }

    // ================================================================
    // STEP 0 / SUB-STEP 3 — BOUNDARY PROOFS
    //
    // The three hardest issues, proven at the raw-parser level (not full
    // binding) BEFORE committing to the full Option C effort:
    //   - collision: same local name as attribute AND element (issue 1, criterion 13)
    //   - namespace URI surfaced (issue 6)
    //   - document order preserved for interleaved repeats (issues 2/5)
    // ================================================================

    /**
     * BOUNDARY PROOF — issue 1 / criterion 13 (THE bug being fixed).
     *
     * <p>An attribute and a child element share the same local name in one
     * element: {@code <RepoTransactionLeg id="ATTR"><id>ELEM</id>}. Jackson
     * collapses both onto one "property" and one clobbers the other; raw StAX
     * exposes them on independent channels ({@code getAttributeValue} vs
     * {@code getElementText}). If this fails, the whole migration is pointless —
     * so this is the single most important assertion in the spike.
     */
    @Test
    void collisionSameLocalNameAttributeAndElement() throws XMLStreamException {
        // Mirrors FiML RepoTransactionLeg / Transfer: attribute `id` + element `id`.
        String xml = "<RepoTransactionLeg id=\"ATTR_ID\"><id>ELEM_ID</id></RepoTransactionLeg>";

        String attrId = null;
        String elemId = null;
        XMLStreamReader reader = INPUT_FACTORY.createXMLStreamReader(new StringReader(xml));
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String localName = reader.getLocalName();
                if ("RepoTransactionLeg".equals(localName)) {
                    attrId = reader.getAttributeValue(null, "id"); // attribute channel
                } else if ("id".equals(localName)) {
                    elemId = reader.getElementText();              // element channel
                }
            }
        }
        reader.close();

        assertEquals("ATTR_ID", attrId, "attribute `id` must survive independently");
        assertEquals("ELEM_ID", elemId, "element `id` must survive independently");
        assertNotEquals(attrId, elemId,
                "attribute and same-named element must NOT collapse onto each other");
    }

    /**
     * BOUNDARY PROOF — issue 6 (namespace, "first wins" today).
     *
     * <p>Two elements share local name {@code commodityOption} but live in
     * different namespaces (FiML vs FpML). The Jackson path loses the namespace
     * (forcing the {@code RoutingInput.Namespace.UNKNOWN} fallback); raw StAX
     * surfaces the real namespace URI per element via {@code getNamespaceURI()},
     * which is exactly what the already-namespace-aware {@code SubstitutionMap}
     * needs fed natively.
     */
    @Test
    void namespaceUriIsSurfacedPerElement() throws XMLStreamException {
        String fimlNs = "http://www.fpml.org/fiml-5-4";
        String fpmlNs = "http://www.fpml.org/FpML-5/confirmation";
        String xml =
                "<root xmlns:fiml=\"" + fimlNs + "\" xmlns:fpml=\"" + fpmlNs + "\">"
                        + "<fiml:commodityOption>FIML</fiml:commodityOption>"
                        + "<fpml:commodityOption>FPML</fpml:commodityOption>"
                        + "</root>";

        List<String> nsByOrder = new ArrayList<>();
        List<String> textByOrder = new ArrayList<>();
        XMLStreamReader reader = INPUT_FACTORY.createXMLStreamReader(new StringReader(xml));
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT
                    && "commodityOption".equals(reader.getLocalName())) {
                nsByOrder.add(reader.getNamespaceURI()); // namespace surfaced natively
                textByOrder.add(reader.getElementText());
            }
        }
        reader.close();

        assertEquals(Arrays.asList(fimlNs, fpmlNs), nsByOrder,
                "each same-local-name element must resolve to its own namespace URI");
        assertEquals(Arrays.asList("FIML", "FPML"), textByOrder,
                "namespace must distinguish the two elements (not 'first wins')");
    }

    /**
     * BOUNDARY PROOF — issues 2/5 (document order / interleaved repeats).
     *
     * <p>The content-model matcher's SEQUENCE handling depends on position.
     * Jackson dumps children into an order-blind property map; raw StAX yields
     * children strictly in document order, so interleaved repeats keep their
     * sequence and nothing collapses to "first only".
     */
    @Test
    void documentOrderPreservedForInterleavedRepeats() throws XMLStreamException {
        String xml = "<root><a>1</a><b>x</b><a>2</a><b>y</b></root>";

        List<String> inOrder = new ArrayList<>();
        XMLStreamReader reader = INPUT_FACTORY.createXMLStreamReader(new StringReader(xml));
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT
                    && !"root".equals(reader.getLocalName())) {
                inOrder.add(reader.getLocalName() + "=" + reader.getElementText());
            }
        }
        reader.close();

        assertEquals(Arrays.asList("a=1", "b=x", "a=2", "b=y"), inOrder,
                "interleaved repeats must keep document order and not collapse");
    }
}
