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

/**
 * Returns the value of an {@code xsi:type} attribute with its namespace prefix resolved, in the form
 * {@code {namespace}localName}, e.g. {@code xsi:type="ext:Square"} reads as {@code {urn:my.extension}Square}.
 *
 * <p>A prefix can only be resolved while the reader is on the element that carries it. Resolving it here
 * means the value still identifies the type after Jackson has buffered the element's tokens, which it
 * does for the children of a type with a VIRTUAL attribute or a content model.
 *
 * <p>Every value comes back in that form, so one the document wrote cannot pass for a resolved one. A value
 * without a namespace, or whose prefix is not declared, comes back as {@code {}} followed by the trimmed
 * value; an undeclared prefix keeps its colon, which shows it was not resolved.
 */
final class XsiTypeResolvingStreamReader extends StreamReader2Delegate {

    XsiTypeResolvingStreamReader(XMLStreamReader2 reader) {
        super(reader);
    }

    @Override
    public String getAttributeValue(int index) {
        String value = super.getAttributeValue(index);
        if (isXsiType(getAttributeNamespace(index), getAttributeLocalName(index))) {
            return resolve(value);
        }
        return value;
    }

    @Override
    public String getAttributeValue(String namespaceURI, String localName) {
        String value = super.getAttributeValue(namespaceURI, localName);
        if (isXsiType(namespaceURI, localName)) {
            return resolve(value);
        }
        return value;
    }

    private static boolean isXsiType(String namespaceURI, String localName) {
        return "type".equals(localName) && XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI.equals(namespaceURI);
    }

    private String resolve(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        int colon = trimmed.indexOf(':');
        String prefix = colon < 0 ? XMLConstants.DEFAULT_NS_PREFIX : trimmed.substring(0, colon);
        String namespaceURI = getNamespaceURI(prefix);
        if (namespaceURI == null || namespaceURI.isEmpty()) {
            return "{}" + trimmed;
        }
        return "{" + namespaceURI + "}" + trimmed.substring(colon + 1);
    }
}
