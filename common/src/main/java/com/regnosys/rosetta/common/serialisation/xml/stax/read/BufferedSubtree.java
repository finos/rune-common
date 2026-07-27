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

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.List;

/**
 * One XML element subtree captured from an {@link XmlCursor} as a flat, replayable event list.
 *
 * <p>Content-model disambiguation cannot decide where a child element belongs until it has seen all
 * of the parent's children, and a StAX cursor cannot rewind — so the routed path buffers each child
 * subtree here and replays it once its destination is known. Unlike Jackson's {@code TokenBuffer},
 * which flattens XML onto a JSON-shaped token stream and loses the namespace context (the root cause
 * of issue 6), this buffer keeps element names, namespace URIs, attributes and document order
 * exactly as the parser reported them.</p>
 */
final class BufferedSubtree {

    private final List<Event> events;

    private BufferedSubtree(List<Event> events) {
        this.events = events;
    }

    /**
     * Captures the element the cursor is currently positioned on, including all descendants.
     *
     * <p>Contract: the cursor must be on the element's {@code START_ELEMENT}; on return it is on the
     * matching {@code END_ELEMENT} — the same contract as
     * {@code StaxReader.readObject}, so callers can treat capture as a drop-in for reading.</p>
     */
    static BufferedSubtree capture(XmlCursor cursor) throws XMLStreamException {
        List<Event> events = new ArrayList<Event>();
        events.add(Event.start(cursor));
        int depth = 1;
        while (depth > 0 && cursor.hasNext()) {
            int type = cursor.next();
            if (type == XMLStreamConstants.START_ELEMENT) {
                events.add(Event.start(cursor));
                depth++;
            } else if (type == XMLStreamConstants.END_ELEMENT) {
                events.add(Event.end());
                depth--;
            } else if (type == XMLStreamConstants.CHARACTERS || type == XMLStreamConstants.CDATA) {
                events.add(Event.text(cursor.getText()));
            }
        }
        return new BufferedSubtree(events);
    }

    /** A cursor over this subtree, positioned on the root {@code START_ELEMENT}. */
    XmlCursor cursor() {
        return new BufferedCursor();
    }

    private static final class Event {
        final int type;
        final String localName;
        final String namespaceUri;
        final String[] attributeNames;
        final String[] attributeValues;
        final String text;

        private Event(int type, String localName, String namespaceUri,
                      String[] attributeNames, String[] attributeValues, String text) {
            this.type = type;
            this.localName = localName;
            this.namespaceUri = namespaceUri;
            this.attributeNames = attributeNames;
            this.attributeValues = attributeValues;
            this.text = text;
        }

        static Event start(XmlCursor cursor) {
            int count = cursor.getAttributeCount();
            String[] names = new String[count];
            String[] values = new String[count];
            for (int i = 0; i < count; i++) {
                names[i] = cursor.getAttributeLocalName(i);
                values[i] = cursor.getAttributeValue(i);
            }
            return new Event(XMLStreamConstants.START_ELEMENT, cursor.getLocalName(),
                    cursor.getNamespaceURI(), names, values, null);
        }

        static Event end() {
            return new Event(XMLStreamConstants.END_ELEMENT, null, null, null, null, null);
        }

        static Event text(String text) {
            return new Event(XMLStreamConstants.CHARACTERS, null, null, null, null, text);
        }
    }

    /**
     * Replays the captured events. The cursor starts on the root {@code START_ELEMENT} (index 0) and
     * ends on the root {@code END_ELEMENT} (the final event), so a reader loop that breaks on
     * {@code END_ELEMENT} terminates exactly where it would on live XML.
     */
    private final class BufferedCursor implements XmlCursor {

        private int index;

        @Override
        public boolean hasNext() {
            return index < events.size() - 1;
        }

        @Override
        public int next() throws XMLStreamException {
            if (!hasNext()) {
                throw new XMLStreamException("No more events in buffered subtree");
            }
            index++;
            return events.get(index).type;
        }

        @Override
        public int getEventType() {
            return events.get(index).type;
        }

        @Override
        public String getLocalName() {
            return events.get(index).localName;
        }

        @Override
        public String getNamespaceURI() {
            return events.get(index).namespaceUri;
        }

        @Override
        public int getAttributeCount() {
            String[] names = events.get(index).attributeNames;
            return names == null ? 0 : names.length;
        }

        @Override
        public String getAttributeLocalName(int attributeIndex) {
            return events.get(index).attributeNames[attributeIndex];
        }

        @Override
        public String getAttributeValue(int attributeIndex) {
            return events.get(index).attributeValues[attributeIndex];
        }

        @Override
        public String getText() {
            return events.get(index).text;
        }

        @Override
        public String getElementText() throws XMLStreamException {
            StringBuilder text = new StringBuilder();
            for (int i = index + 1; i < events.size(); i++) {
                Event event = events.get(i);
                if (event.type == XMLStreamConstants.START_ELEMENT) {
                    throw new XMLStreamException(
                            "Element has child element <" + event.localName + ">, expected text only");
                }
                if (event.type == XMLStreamConstants.END_ELEMENT) {
                    index = i;
                    return text.toString();
                }
                text.append(event.text);
            }
            throw new XMLStreamException("Unterminated element in buffered subtree");
        }
    }
}
