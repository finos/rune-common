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

import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.dataformat.xml.XmlNameProcessor;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;
import com.fasterxml.jackson.dataformat.xml.deser.XmlTokenStream;

import javax.xml.stream.XMLStreamReader;
import java.io.IOException;

/**
 * The parser {@link RosettaXmlFactory} creates. It can tell whether the StAX reader is on the element
 * whose {@link JsonToken#START_OBJECT} was just returned.
 */
public final class RosettaXmlParser extends FromXmlParser {

    RosettaXmlParser(IOContext ctxt, int genericParserFeatures, int xmlFeatures, ObjectCodec codec,
                     XMLStreamReader xmlReader, XmlNameProcessor tagProcessor) throws IOException {
        super(ctxt, genericParserFeatures, xmlFeatures, codec, xmlReader, tagProcessor);
    }

    /**
     * Whether the StAX reader is on the element whose {@code START_OBJECT} is the current token, so that
     * the reader's attributes are that element's.
     *
     * <p>Jackson returns the {@code START_OBJECT} of the root element while still on its start tag, and that
     * of any other element with attributes while about to read the first of them. It returns the
     * {@code START_OBJECT} of a non-root element without attributes only once the reader has moved on to the
     * element's first child or text. Such an element has no attributes to read anyway.
     */
    public boolean readerIsOnStartedElement() {
        if (!hasToken(JsonToken.START_OBJECT)) {
            return false;
        }
        int state = _xmlTokens.getCurrentToken();
        if (state == XmlTokenStream.XML_ATTRIBUTE_NAME) {
            return true;
        }
        JsonStreamContext parent = _parsingContext.getParent();
        return state == XmlTokenStream.XML_START_ELEMENT && parent != null && parent.inRoot();
    }
}
