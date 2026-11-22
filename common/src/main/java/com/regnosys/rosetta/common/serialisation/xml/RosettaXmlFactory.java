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
 * An {@link XmlFactory} whose parsers expose an element's {@code xsi:type} attribute as the field
 * {@code xsi:type}, with its namespace prefix resolved (see {@link XsiTypeResolvingStreamReader}).
 *
 * <p>The field name cannot clash with an attribute or element of the model, since an XML name has no
 * prefix once Jackson has read it.
 */
final class RosettaXmlFactory extends XmlFactory {
    private static final long serialVersionUID = 1L;

    RosettaXmlFactory() {
        super();
        _xmlParserFeatures |= FromXmlParser.Feature.AUTO_DETECT_XSI_TYPE.getMask();
    }

    private RosettaXmlFactory(RosettaXmlFactory src, ObjectCodec codec) {
        super(src, codec);
    }

    @Override
    public XmlFactory copy() {
        return new RosettaXmlFactory(this, null);
    }

    @Override
    protected FromXmlParser createParser(IOContext ctxt, int genericParserFeatures, int xmlFeatures, ObjectCodec codec,
                                         XMLStreamReader xmlReader, XmlNameProcessor tagProcessor) throws IOException {
        XMLStreamReader resolving = new XsiTypeResolvingStreamReader(Stax2JacksonReaderAdapter.wrapIfNecessary(xmlReader));
        return new RosettaXmlParser(ctxt, genericParserFeatures, xmlFeatures, codec, resolving, tagProcessor);
    }
}
