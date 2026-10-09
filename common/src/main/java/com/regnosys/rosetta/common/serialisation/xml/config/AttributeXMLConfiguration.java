package com.regnosys.rosetta.common.serialisation.xml.config;

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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class AttributeXMLConfiguration {
	private final Optional<String> xmlName;
	private final Optional<Map<String, String>> xmlAttributes;
	private final Optional<AttributeXMLRepresentation> xmlRepresentation;
	@Deprecated
	private final Optional<String> substitutionGroup;
	private final Optional<String> elementRef;
	private final Optional<Boolean> xmlList;

	public AttributeXMLConfiguration(
            Optional<String> xmlName,
            Optional<Map<String, String>> xmlAttributes,
            Optional<AttributeXMLRepresentation> xmlRepresentation,
            Optional<String> substitutionGroup,
			Optional<String> elementRef) {
		this(xmlName, xmlAttributes, xmlRepresentation, substitutionGroup, elementRef, Optional.empty());
	}

	@JsonCreator
	public AttributeXMLConfiguration(
            @JsonProperty("xmlName") Optional<String> xmlName,
            @JsonProperty("xmlAttributes") Optional<Map<String, String>> xmlAttributes,
            @JsonProperty("xmlRepresentation") Optional<AttributeXMLRepresentation> xmlRepresentation,
            @JsonProperty("substitutionGroup") Optional<String> substitutionGroup,
			@JsonProperty("elementRef") Optional<String> elementRef,
			@JsonProperty("xmlList") Optional<Boolean> xmlList) {
		this.xmlName = xmlName;
		this.xmlAttributes = xmlAttributes;
		this.xmlRepresentation = xmlRepresentation;
		this.substitutionGroup = substitutionGroup;
        this.elementRef = elementRef;
		this.xmlList = xmlList;
    }

	public Optional<String> getXmlName() {
		return xmlName;
	}

	public Optional<Map<String, String>> getXmlAttributes() {
		return xmlAttributes;
	}

	public Optional<AttributeXMLRepresentation> getXmlRepresentation() {
		return xmlRepresentation;
	}

	/**
	 * @deprecated this is a legacy field as this isn't actually a substitution group it points to rather an elementRef, the getElementRef() method should be used instead
	 */
	@Deprecated
	public Optional<String> getSubstitutionGroup() {
		return substitutionGroup;
	}

	public Optional<String> getElementRef() {
		return elementRef;
	}

	/**
	 * Whether the value is an XSD list type: the attribute's items written as one whitespace-separated
	 * string, in an XML attribute or as element text. Only valid on a multi-cardinality attribute.
	 */
	public Optional<Boolean> getXmlList() {
		return xmlList;
	}

	@Override
	public int hashCode() {
		return Objects.hash(xmlAttributes, xmlName, xmlRepresentation, substitutionGroup, elementRef, xmlList);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AttributeXMLConfiguration other = (AttributeXMLConfiguration) obj;
		return Objects.equals(xmlAttributes, other.xmlAttributes)
				&& Objects.equals(xmlName, other.xmlName) && Objects.equals(xmlRepresentation, other.xmlRepresentation)
				&& Objects.equals(substitutionGroup, other.substitutionGroup)
				&& Objects.equals(elementRef, other.elementRef)
				&& Objects.equals(xmlList, other.xmlList);
	}
}
