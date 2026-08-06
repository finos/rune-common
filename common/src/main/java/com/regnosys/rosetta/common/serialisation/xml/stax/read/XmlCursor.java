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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * The minimal pull-parser surface {@link StaxReader} needs, so that the reader can consume either a
 * live {@link XMLStreamReader} or a buffered subtree with one code path.
 *
 * <p>Content-model disambiguation (Step 4c) has to see all of an element's children before it can
 * decide where each one belongs, which means buffering them; a buffered subtree is then replayed
 * through exactly the same reader logic as live XML. Method semantics deliberately match
 * {@link XMLStreamReader} — in particular {@link #getElementText()} leaves the cursor on the
 * element's {@code END_ELEMENT}.</p>
 */
interface XmlCursor {

    boolean hasNext() throws XMLStreamException;

    /** Advances to the next event and returns its {@code XMLStreamConstants} type. */
    int next() throws XMLStreamException;

    int getEventType();

    String getLocalName();

    /** Namespace URI of the current element, or {@code null} when it has none. */
    String getNamespaceURI();

    /** Number of XML attributes on the current {@code START_ELEMENT} (namespace declarations excluded). */
    int getAttributeCount();

    String getAttributeLocalName(int index);

    String getAttributeValue(int index);

    /** Text of the current {@code CHARACTERS} / {@code CDATA} event. */
    String getText();

    /**
     * Reads the text content of the current element, leaving the cursor on its {@code END_ELEMENT}.
     *
     * @throws XMLStreamException if the element has child elements
     */
    String getElementText() throws XMLStreamException;
}
