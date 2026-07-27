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
 * {@link XmlCursor} over a live StAX {@link XMLStreamReader}. A thin pass-through: every method
 * delegates directly, so reading live XML costs nothing beyond one virtual call.
 */
final class StaxCursor implements XmlCursor {

    private final XMLStreamReader reader;

    StaxCursor(XMLStreamReader reader) {
        this.reader = reader;
    }

    @Override
    public boolean hasNext() throws XMLStreamException {
        return reader.hasNext();
    }

    @Override
    public int next() throws XMLStreamException {
        return reader.next();
    }

    @Override
    public int getEventType() {
        return reader.getEventType();
    }

    @Override
    public String getLocalName() {
        return reader.getLocalName();
    }

    @Override
    public String getNamespaceURI() {
        return reader.getNamespaceURI();
    }

    @Override
    public int getAttributeCount() {
        return reader.getAttributeCount();
    }

    @Override
    public String getAttributeLocalName(int index) {
        return reader.getAttributeLocalName(index);
    }

    @Override
    public String getAttributeValue(int index) {
        return reader.getAttributeValue(index);
    }

    @Override
    public String getText() {
        return reader.getText();
    }

    @Override
    public String getElementText() throws XMLStreamException {
        return reader.getElementText();
    }
}
