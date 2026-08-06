package com.regnosys.rosetta.common.serialisation.xml.stax.introspect;

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

import com.regnosys.rosetta.common.serialisation.xml.config.AttributeXMLRepresentation;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * The XML binding plan for one attribute of a Rune-generated type.
 * Produced by {@link RuneTypeIntrospector}; consumed by the StAX writer and reader.
 */
public class AttributeBinding {

    private final String logicalName;
    private final Method getter;
    private final Method setter;
    private final Method adder;
    private final boolean isMulti;
    private final Class<?> valueType;
    private final boolean isRosettaModelObject;
    private final boolean isEnum;
    private final String xmlName;
    private final AttributeXMLRepresentation xmlRepresentation;
    private final Optional<String> elementRef;

    public AttributeBinding(
            String logicalName,
            Method getter,
            Method setter,
            Method adder,
            boolean isMulti,
            Class<?> valueType,
            boolean isRosettaModelObject,
            boolean isEnum,
            String xmlName,
            AttributeXMLRepresentation xmlRepresentation,
            Optional<String> elementRef) {
        this.logicalName = logicalName;
        this.getter = getter;
        this.setter = setter;
        this.adder = adder;
        this.isMulti = isMulti;
        this.valueType = valueType;
        this.isRosettaModelObject = isRosettaModelObject;
        this.isEnum = isEnum;
        this.xmlName = xmlName;
        this.xmlRepresentation = xmlRepresentation;
        this.elementRef = elementRef;
    }

    /** Rune logical name from {@code @RosettaAttribute.value()}. */
    public String getLogicalName() {
        return logicalName;
    }

    /** Getter method on the impl/builder class. */
    public Method getGetter() {
        return getter;
    }

    /**
     * Setter on the builder class.
     * {@code null} for multi-cardinality attributes — use {@link #getAdder()} instead.
     */
    public Method getSetter() {
        return setter;
    }

    /**
     * Single-element adder on the builder class (e.g., {@code addFoo(Foo)}).
     * {@code null} for single-cardinality attributes — use {@link #getSetter()} instead.
     */
    public Method getAdder() {
        return adder;
    }

    /** {@code true} when the attribute carries {@code @Multi} — a {@code List<?>} in Java. */
    public boolean isMulti() {
        return isMulti;
    }

    /** Actual Rune type of the attribute value (unwrapped from {@code List} and builder). */
    public Class<?> getValueType() {
        return valueType;
    }

    /** {@code true} when {@link #getValueType()} implements {@code RosettaModelObject}. */
    public boolean isRosettaModelObject() {
        return isRosettaModelObject;
    }

    /** {@code true} when {@link #getValueType()} is a Java enum. */
    public boolean isEnum() {
        return isEnum;
    }

    /** XML element / attribute local name, resolved from config or defaulting to {@link #getLogicalName()}. */
    public String getXmlName() {
        return xmlName;
    }

    /** How this attribute is represented in XML: ELEMENT, ATTRIBUTE, VALUE, or VIRTUAL. */
    public AttributeXMLRepresentation getXmlRepresentation() {
        return xmlRepresentation;
    }

    /**
     * Substitution-group element reference from config ({@code elementRef} field,
     * falling back to the deprecated {@code substitutionGroup} field).
     * Present only when this attribute holds a substitution-group head.
     */
    public Optional<String> getElementRef() {
        return elementRef;
    }
}
