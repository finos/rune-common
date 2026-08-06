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
import com.regnosys.rosetta.common.serialisation.xml.config.RosettaXMLConfiguration;
import com.rosetta.test.Camel;
import com.rosetta.test.DocumentExtension;
import com.rosetta.test.Fish;
import com.rosetta.test.Foo;
import com.rosetta.test.Measure;
import com.rosetta.test.MulticardinalityContainer;
import com.rosetta.test.Party;
import com.rosetta.test.TypeWithTypeElement;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RuneTypeIntrospectorTest {

    private static RosettaXMLConfiguration config;
    private static RuneTypeIntrospector introspector;

    @BeforeAll
    static void setUp() throws IOException {
        try (InputStream is = RuneTypeIntrospectorTest.class
                .getResourceAsStream("/serialisation/xml/xml-config/my-schema-xml-config.json")) {
            config = RosettaXMLConfiguration.load(is);
        }
        introspector = new RuneTypeIntrospector();
    }

    // -------------------------------------------------------------------------
    // Attribute order and representations
    // -------------------------------------------------------------------------

    @Test
    void measureAttributeOrderAndRepresentation() {
        TypeBinding binding = introspector.introspect(Measure.class, config);
        List<AttributeBinding> attrs = binding.getAttributes();
        assertEquals(2, attrs.size());

        AttributeBinding value = attrs.get(0);
        assertEquals("value", value.getLogicalName());
        assertEquals("value", value.getXmlName());
        assertEquals(AttributeXMLRepresentation.VALUE, value.getXmlRepresentation());
        assertFalse(value.isMulti());
        assertFalse(value.isEnum());
        assertFalse(value.isRosettaModelObject());
        assertNotNull(value.getSetter(), "single-cardinality attribute must have a setter");
        assertNull(value.getAdder());

        AttributeBinding unit = attrs.get(1);
        assertEquals("unit", unit.getLogicalName());
        assertEquals("Unit", unit.getXmlName(), "config xmlName override must apply");
        assertEquals(AttributeXMLRepresentation.ATTRIBUTE, unit.getXmlRepresentation());
        assertFalse(unit.isMulti());
        assertTrue(unit.isEnum(), "UnitEnum is an enum");
        assertFalse(unit.isRosettaModelObject());
        assertNotNull(unit.getSetter());
        assertNull(unit.getAdder());
    }

    @Test
    void multiCardinalityAdderDiscovery() {
        TypeBinding binding = introspector.introspect(MulticardinalityContainer.class, config);
        List<AttributeBinding> attrs = binding.getAttributes();
        assertEquals(1, attrs.size());

        AttributeBinding foo = attrs.get(0);
        assertEquals("foo", foo.getLogicalName());
        assertTrue(foo.isMulti());
        assertEquals(Foo.class, foo.getValueType());
        assertTrue(foo.isRosettaModelObject(), "Foo implements RosettaModelObject");
        assertNull(foo.getSetter(), "multi-cardinality attribute has no setter");
        assertNotNull(foo.getAdder(), "multi-cardinality attribute must have an adder");
        assertEquals("addFoo", foo.getAdder().getName());
    }

    @Test
    void virtualRepresentation() {
        TypeBinding binding = introspector.introspect(Party.class, config);
        List<AttributeBinding> attrs = binding.getAttributes();
        assertEquals(2, attrs.size());

        assertEquals("partyNameModel", attrs.get(0).getLogicalName());
        assertEquals(AttributeXMLRepresentation.VIRTUAL, attrs.get(0).getXmlRepresentation());
        assertTrue(attrs.get(0).isRosettaModelObject());

        assertEquals("partyModel", attrs.get(1).getLogicalName());
        assertEquals(AttributeXMLRepresentation.VIRTUAL, attrs.get(1).getXmlRepresentation());
    }

    // -------------------------------------------------------------------------
    // Inheritance and declaration order
    // -------------------------------------------------------------------------

    @Test
    void inheritanceAttributeOrder() {
        // DocumentExtension extends Document; Document has [attr, value]; DocumentExtension adds [documentExtensionAttr]
        TypeBinding binding = introspector.introspect(DocumentExtension.class, config);
        List<AttributeBinding> attrs = binding.getAttributes();
        assertEquals(3, attrs.size(),
                "Expected parent [attr, value] then child [documentExtensionAttr]");

        assertEquals("attr", attrs.get(0).getLogicalName());
        assertEquals("Attr", attrs.get(0).getXmlName());

        assertEquals("value", attrs.get(1).getLogicalName());
        assertEquals("Value", attrs.get(1).getXmlName());

        assertEquals("documentExtensionAttr", attrs.get(2).getLogicalName());
        assertEquals("DocumentExtensionAttr", attrs.get(2).getXmlName());
    }

    @Test
    void inheritedAttributeConfigFromParent() {
        // Camel extends Animal; Animal's config has name = ATTRIBUTE; Camel has no attributes config.
        TypeBinding binding = introspector.introspect(Camel.class, config);
        List<AttributeBinding> attrs = binding.getAttributes();
        assertEquals(1, attrs.size());

        AttributeBinding name = attrs.get(0);
        assertEquals("name", name.getLogicalName());
        assertEquals(AttributeXMLRepresentation.ATTRIBUTE, name.getXmlRepresentation(),
                "ATTRIBUTE representation must be resolved from the parent type's config");
    }

    // -------------------------------------------------------------------------
    // getType() and _getType() collision
    // -------------------------------------------------------------------------

    @Test
    void getTypeMethodExcluded() {
        // getType() carries @RuneAttribute("@type") but NOT @RosettaAttribute — must be excluded.
        TypeBinding binding = introspector.introspect(Measure.class, config);
        for (AttributeBinding attr : binding.getAttributes()) {
            assertFalse("getType".equals(attr.getGetter().getName()),
                    "getType() must not appear in attribute bindings");
            assertFalse("@type".equals(attr.getLogicalName()),
                    "@type virtual attribute must not appear in attribute bindings");
        }
    }

    @Test
    void typeElementNameClash() {
        // TypeWithTypeElement has a Rune attribute named "type" whose getter is _getType().
        // This must be distinct from the structural getType() method.
        TypeBinding binding = introspector.introspect(TypeWithTypeElement.class, config);
        List<AttributeBinding> attrs = binding.getAttributes();
        assertEquals(2, attrs.size());

        assertEquals("firstElement", attrs.get(0).getLogicalName());
        assertEquals(String.class, attrs.get(0).getValueType());

        AttributeBinding typeAttr = attrs.get(1);
        assertEquals("type", typeAttr.getLogicalName());
        assertEquals(String.class, typeAttr.getValueType());
        assertEquals("_getType", typeAttr.getGetter().getName(),
                "getter for the 'type' Rune attribute is _getType(), not getType()");
    }

    // -------------------------------------------------------------------------
    // Type-level XML metadata
    // -------------------------------------------------------------------------

    @Test
    void typeXmlElementNameAndNamespace() {
        TypeBinding binding = introspector.introspect(Camel.class, config);
        assertEquals("camel", binding.getXmlElementName());
        assertTrue(binding.getXmlElementNamespace().isPresent());
        assertEquals("urn:my.schema", binding.getXmlElementNamespace().get());
        assertFalse(binding.isAbstract());
    }

    @Test
    void abstractType() {
        TypeBinding binding = introspector.introspect(Fish.class, config);
        assertTrue(binding.isAbstract());
        assertEquals("fish", binding.getXmlElementName());
    }

    @Test
    void typeWithNoConfigEntry() {
        // TypeWithTypeElement is not in the XML config — should use defaults.
        TypeBinding binding = introspector.introspect(TypeWithTypeElement.class, config);

        assertEquals("TypeWithTypeElement", binding.getXmlElementName(),
                "type name must default to the logical name from @RuneDataType");
        assertFalse(binding.getXmlElementNamespace().isPresent());
        assertFalse(binding.isAbstract());
        assertTrue(binding.getXmlConstantAttributes().isEmpty());
        assertFalse(binding.getContentModel().isPresent());

        // All attributes default to ELEMENT representation
        for (AttributeBinding attr : binding.getAttributes()) {
            assertEquals(AttributeXMLRepresentation.ELEMENT, attr.getXmlRepresentation());
            assertEquals(attr.getLogicalName(), attr.getXmlName(),
                    "xmlName must default to logicalName when no config is present");
        }
    }
}
