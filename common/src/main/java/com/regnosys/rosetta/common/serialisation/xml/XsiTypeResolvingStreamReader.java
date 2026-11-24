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

import org.codehaus.stax2.XMLStreamReader2;
import org.codehaus.stax2.util.StreamReader2Delegate;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

/**
 * Adds, to an element that has an {@code xsi:type} attribute, one more attribute named
 * {@link XsiTypeValue#FIELD_NAME} whose value is that xsi:type with its namespace prefix resolved (see
 * {@link XsiTypeValue}), e.g. {@code xsi:type="ext:Square"} adds {@code {urn:my.extension}Square}. The
 * xsi:type attribute itself is left as it is, so a model property named {@code type} still receives it.
 *
 * <p>{@link RosettaXmlFactory} wraps every StAX reader its parsers read from in this one. A prefix can only
 * be resolved while the reader is on the element that carries it. Resolving it here means the value still
 * identifies the type after Jackson has buffered the element's tokens, which it does for the children of a
 * type with a VIRTUAL attribute or a content model.
 */
final class XsiTypeResolvingStreamReader extends StreamReader2Delegate {

    XsiTypeResolvingStreamReader(XMLStreamReader2 reader) {
        super(reader);
    }

    @Override
    public int getAttributeCount() {
        int count = super.getAttributeCount();
        return xsiType() == null ? count : count + 1;
    }

    @Override
    public String getAttributeLocalName(int index) {
        return isAdded(index) ? XsiTypeValue.FIELD_NAME : super.getAttributeLocalName(index);
    }

    @Override
    public String getAttributeNamespace(int index) {
        return isAdded(index) ? XMLConstants.NULL_NS_URI : super.getAttributeNamespace(index);
    }

    @Override
    public String getAttributePrefix(int index) {
        return isAdded(index) ? XMLConstants.DEFAULT_NS_PREFIX : super.getAttributePrefix(index);
    }

    @Override
    public QName getAttributeName(int index) {
        return isAdded(index) ? new QName(XsiTypeValue.FIELD_NAME) : super.getAttributeName(index);
    }

    @Override
    public String getAttributeType(int index) {
        return isAdded(index) ? "CDATA" : super.getAttributeType(index);
    }

    @Override
    public boolean isAttributeSpecified(int index) {
        return !isAdded(index) && super.isAttributeSpecified(index);
    }

    @Override
    public String getAttributeValue(int index) {
        return isAdded(index) ? resolve(xsiType()) : super.getAttributeValue(index);
    }

    @Override
    public String getAttributeValue(String namespaceURI, String localName) {
        if (XsiTypeValue.FIELD_NAME.equals(localName) && (namespaceURI == null || namespaceURI.isEmpty())) {
            String xsiType = xsiType();
            return xsiType == null ? null : resolve(xsiType);
        }
        return super.getAttributeValue(namespaceURI, localName);
    }

    // The added attribute comes after the element's own ones.
    private boolean isAdded(int index) {
        return index == super.getAttributeCount() && xsiType() != null;
    }

    private String xsiType() {
        return super.getAttributeValue(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "type");
    }

    private String resolve(String value) {
        String trimmed = value.trim();
        int colon = trimmed.indexOf(':');
        String prefix = colon < 0 ? XMLConstants.DEFAULT_NS_PREFIX : trimmed.substring(0, colon);
        String localName = trimmed.substring(colon + 1);
        if (localName.isEmpty() || localName.indexOf(':') >= 0 || (colon >= 0 && prefix.isEmpty())) {
            return XsiTypeValue.unresolved(value);
        }
        String namespaceURI = getNamespaceURI(prefix);
        if (namespaceURI == null || namespaceURI.isEmpty()) {
            // Unprefixed with no default namespace names a type in no namespace; a prefix must be declared.
            return colon < 0 ? XsiTypeValue.resolved(null, localName) : XsiTypeValue.unresolved(value);
        }
        return XsiTypeValue.resolved(namespaceURI, localName);
    }
}
