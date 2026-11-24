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

import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlNameProcessor;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;
import com.fasterxml.jackson.dataformat.xml.util.Stax2JacksonReaderAdapter;

import javax.xml.stream.XMLStreamReader;
import java.io.IOException;

/**
 * The {@link XmlFactory} of {@link RosettaXmlMapper}. Its parsers are {@link RosettaXmlParser}s, which can
 * tell whether the StAX reader is on the element just started, and read through an
 * {@link XsiTypeResolvingStreamReader}, which gives an element's xsi:type with its prefix resolved as an
 * extra {@link XsiTypeValue#FIELD_NAME} attribute. {@code XsiTypeDeserializer} relies on both.
 */
final class RosettaXmlFactory extends XmlFactory {
    private static final long serialVersionUID = 1L;

    /** Without any XSD type names nothing selects a type by xsi:type, so parsers don't add the resolved attribute. */
    private final boolean resolveXsiType;

    RosettaXmlFactory(boolean resolveXsiType) {
        super();
        this.resolveXsiType = resolveXsiType;
    }

    private RosettaXmlFactory(RosettaXmlFactory src, ObjectCodec codec) {
        super(src, codec);
        this.resolveXsiType = src.resolveXsiType;
    }

    @Override
    public XmlFactory copy() {
        return new RosettaXmlFactory(this, null);
    }

    @Override
    protected FromXmlParser createParser(IOContext ctxt, int genericParserFeatures, int xmlFeatures, ObjectCodec codec,
                                         XMLStreamReader xmlReader, XmlNameProcessor tagProcessor) throws IOException {
        XMLStreamReader reader = resolveXsiType
                ? new XsiTypeResolvingStreamReader(Stax2JacksonReaderAdapter.wrapIfNecessary(xmlReader))
                : xmlReader;
        return new RosettaXmlParser(ctxt, genericParserFeatures, xmlFeatures, codec, reader, tagProcessor);
    }
}
