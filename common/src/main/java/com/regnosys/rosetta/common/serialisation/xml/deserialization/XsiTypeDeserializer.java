package com.regnosys.rosetta.common.serialisation.xml.deserialization;

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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.std.DelegatingDeserializer;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.regnosys.rosetta.common.serialisation.xml.RosettaXmlParser;
import com.regnosys.rosetta.common.serialisation.xml.XsiTypeValue;
import com.regnosys.rosetta.common.serialisation.xml.SubstitutionMap.XMLFullyQualifiedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.XMLConstants;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * Reads an element as the subtype its {@code xsi:type} attribute names, instead of the type the
 * element is declared with.
 *
 * <p>The XSD type each Rune type was generated from comes from {@code xmlTypeFullyQualifiedName} in the
 * XML configuration. When {@code xsi:type} names a configured subtype of the type this deserializer
 * wraps, the element is read with that subtype's deserializer. Otherwise it is read as the wrapped type.
 *
 * <p>The xsi:type is read from the attribute {@link XsiTypeValue#FIELD_NAME}, which {@code RosettaXmlFactory}'s
 * parsers add with the prefix already resolved. With a {@link RosettaXmlParser}, the element is read in
 * place, and the value is taken from the StAX reader when the parser confirms the reader is on the element;
 * when it is not, the element has no attributes, so no xsi:type. With any other parser, such as the buffer
 * Jackson reads the children of a type with a VIRTUAL attribute or a content model from, the element is
 * buffered and the value is taken from its own field of that name.
 */
final class XsiTypeDeserializer extends DelegatingDeserializer {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(XsiTypeDeserializer.class);

    private final Class<?> runeType;
    /** The configured XSD type names of {@code runeType} and its subtypes, mapped to their Rune types. */
    private final Map<XMLFullyQualifiedName, Class<?>> typesByXsdTypeName;

    XsiTypeDeserializer(JsonDeserializer<?> delegatee,
                        Class<?> runeType,
                        Map<XMLFullyQualifiedName, Class<?>> typesByXsdTypeName) {
        super(delegatee);
        this.runeType = runeType;
        this.typesByXsdTypeName = typesByXsdTypeName;
    }

    @Override
    protected JsonDeserializer<?> newDelegatingInstance(JsonDeserializer<?> newDelegatee) {
        return new XsiTypeDeserializer(newDelegatee, runeType, typesByXsdTypeName);
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (!p.hasToken(JsonToken.START_OBJECT)) {
            return _delegatee.deserialize(p, ctxt);
        }
        String elementName = elementName(p);
        if (p instanceof RosettaXmlParser) {
            RosettaXmlParser xmlParser = (RosettaXmlParser) p;
            String xsiType = xmlParser.readerIsOnStartedElement()
                    ? xmlParser.getStaxReader().getAttributeValue(XMLConstants.NULL_NS_URI, XsiTypeValue.FIELD_NAME)
                    : null;
            return deserializerFor(xsiType, elementName, ctxt).deserialize(p, ctxt);
        }
        TokenBuffer buffer = ctxt.bufferAsCopyOfValue(p);
        String xsiType = findXsiTypeField(buffer);
        try (JsonParser buffered = buffer.asParserOnFirstToken()) {
            return deserializerFor(xsiType, elementName, ctxt).deserialize(buffered, ctxt);
        }
    }

    // For warnings only. FromXmlParser.currentName() throws on the root element.
    private static String elementName(JsonParser p) {
        if (p instanceof RosettaXmlParser) {
            return ((RosettaXmlParser) p).startedElementName();
        }
        JsonStreamContext parent = p.getParsingContext().getParent();
        return parent == null ? null : parent.getCurrentName();
    }

    private JsonDeserializer<?> deserializerFor(String xsiType, String elementName, DeserializationContext ctxt) throws IOException {
        if (xsiType == null) {
            return _delegatee;
        }
        Class<?> selected = resolve(xsiType, elementName);
        if (selected == null || selected.equals(runeType)) {
            return _delegatee;
        }
        return ctxt.findRootValueDeserializer(ctxt.constructType(selected));
    }

    private String findXsiTypeField(TokenBuffer buffer) throws IOException {
        try (JsonParser scan = buffer.asParserOnFirstToken()) {
            JsonToken token = scan.nextToken();
            while (token == JsonToken.FIELD_NAME) {
                String name = scan.currentName();
                scan.nextToken();
                if (XsiTypeValue.FIELD_NAME.equals(name) && scan.hasToken(JsonToken.VALUE_STRING)) {
                    return scan.getText();
                }
                scan.skipChildren();
                token = scan.nextToken();
            }
        }
        return null;
    }

    private Class<?> resolve(String xsiType, String elementName) {
        Optional<XMLFullyQualifiedName> name = XsiTypeValue.typeName(xsiType);
        if (!name.isPresent()) {
            LOGGER.warn("Reading <{}> as {}: its xsi:type \"{}\" is not a QName with a declared namespace prefix.",
                    elementName, runeType.getName(), XsiTypeValue.describe(xsiType));
            return null;
        }
        Class<?> type = typesByXsdTypeName.get(name.get());
        if (type == null) {
            LOGGER.warn("Reading <{}> as {}: its xsi:type {} is not the XSD type of {} or of a subtype in the XML configuration, "
                            + "so any content that belongs only to that type is ignored.",
                    elementName, runeType.getName(), XsiTypeValue.describe(xsiType), runeType.getName());
        }
        return type;
    }
}
