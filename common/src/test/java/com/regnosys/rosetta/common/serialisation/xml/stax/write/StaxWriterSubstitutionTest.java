package com.regnosys.rosetta.common.serialisation.xml.stax.write;

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

import com.google.common.io.Resources;
import com.regnosys.rosetta.common.serialisation.xml.config.AttributeXMLConfiguration;
import com.regnosys.rosetta.common.serialisation.xml.config.RosettaXMLConfiguration;
import com.regnosys.rosetta.common.serialisation.xml.config.TypeXMLConfiguration;
import com.rosetta.model.lib.ModelSymbolId;
import com.rosetta.test.AnimalContainer;
import com.rosetta.test.Cow;
import com.rosetta.test.Goat;
import com.rosetta.test.Salmon;
import com.rosetta.test.Shark;
import com.rosetta.test.Zoo;
import com.rosetta.util.DottedPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link StaxWriter} covering substitution group serialisation scenarios.
 *
 * <p>Replicates the 5 substitution-group tests from {@code XmlSerialisationTest}
 * using the StAX writer directly.
 */
public class StaxWriterSubstitutionTest {

    private static final String XML_CONFIG =
            "serialisation/xml/xml-config/extension-schema-xml-config.json";

    private URL configUrl;
    private StaxWriter writer;
    private String licenseHeader;

    @BeforeEach
    public void setUp() throws Exception {
        configUrl = Resources.getResource(XML_CONFIG);
        RosettaXMLConfiguration config;
        try (InputStream is = configUrl.openStream()) {
            config = RosettaXMLConfiguration.load(is);
        }
        writer = new StaxWriter(config);
        licenseHeader = Resources.toString(
                Resources.getResource("serialisation/xml/expected/license-header.xml"),
                StandardCharsets.UTF_8);
    }

    @Test
    public void testSubstitutionGroupSerialisation() throws Exception {
        AnimalContainer animalContainer = AnimalContainer.builder()
                .setAnimal(Goat.builder().setName("Goatee").build())
                .build();

        String actual = licenseHeader + writer.write(animalContainer, true,
                Collections.<String, String>emptyMap());
        String expected = Resources.toString(
                Resources.getResource("serialisation/xml/expected/substitution-group.xml"),
                StandardCharsets.UTF_8);
        assertEquals(expected, actual);
    }

    @Test
    public void testMultiCardinalitySubstitutionGroupSerialisation() throws Exception {
        Zoo zoo = Zoo.builder()
                .addAnimal(Goat.builder().setName("Goatee").build())
                .addAnimal(Cow.builder().setName("Moomoo").build())
                .addAnimal(Shark.builder().setName("Jaws").build())
                .addAnimal(Salmon.builder().setName("Sashimi").build())
                .build();

        String actual = licenseHeader + writer.write(zoo, true,
                Collections.<String, String>emptyMap());
        String expected = Resources.toString(
                Resources.getResource("serialisation/xml/expected/substitution-group-multi.xml"),
                StandardCharsets.UTF_8);
        assertEquals(expected, actual);
    }

    @Test
    public void testSubstitutionGroupLegacyV2Serialisation() throws Exception {
        AnimalContainer animalContainer = AnimalContainer.builder()
                .setAnimal(Goat.builder().setName("Goatee").build())
                .build();

        StaxWriter legacyWriter = new StaxWriter(getLegacyV2RosettaXMLConfiguration());
        String actual = licenseHeader + legacyWriter.write(animalContainer, true,
                Collections.<String, String>emptyMap());
        String expected = Resources.toString(
                Resources.getResource("serialisation/xml/expected/substitution-group.xml"),
                StandardCharsets.UTF_8);
        assertEquals(expected, actual);
    }

    @Test
    public void testMultiCardinalitySubstitutionGroupLegacyV2Serialisation() throws Exception {
        Zoo zoo = Zoo.builder()
                .addAnimal(Goat.builder().setName("Goatee").build())
                .addAnimal(Cow.builder().setName("Moomoo").build())
                .build();

        StaxWriter legacyWriter = new StaxWriter(getLegacyV2RosettaXMLConfiguration());
        String actual = licenseHeader + legacyWriter.write(zoo, true,
                Collections.<String, String>emptyMap());
        String expected = Resources.toString(
                Resources.getResource("serialisation/xml/expected/substitution-group-multi-legacy.xml"),
                StandardCharsets.UTF_8);
        assertEquals(expected, actual);
    }

    @Test
    public void testSubstitutionGroupLegacyV1Serialisation() throws Exception {
        AnimalContainer animalContainer = AnimalContainer.builder()
                .setAnimal(Goat.builder().setName("Goatee").build())
                .build();

        StaxWriter legacyWriter = new StaxWriter(getLegacyV1RosettaXMLConfiguration());
        String actual = licenseHeader + legacyWriter.write(animalContainer, true,
                Collections.<String, String>emptyMap());
        String expected = Resources.toString(
                Resources.getResource("serialisation/xml/expected/substitution-group.xml"),
                StandardCharsets.UTF_8);
        assertEquals(expected, actual);
    }

    // -------------------------------------------------------------------------
    // Legacy config builders — replicated from XmlSerialisationTest
    // -------------------------------------------------------------------------

    private RosettaXMLConfiguration getLegacyV2RosettaXMLConfiguration() {
        try (InputStream inputStream = configUrl.openStream()) {
            final RosettaXMLConfiguration config = RosettaXMLConfiguration.load(inputStream);
            Map<ModelSymbolId, TypeXMLConfiguration> newTypeConfigMap = new HashMap<>();

            config.getTypeConfigMap().forEach((modelSymbolId, typeXMLConfiguration) -> {
                if (!typeXMLConfiguration.getAbstract().orElse(false)) {
                    Optional<Map<String, AttributeXMLConfiguration>> newAttributeXmlConfiguration =
                            typeXMLConfiguration.getAttributes()
                                    .map(attributes -> {
                                        Map<String, AttributeXMLConfiguration> newAttributes = new HashMap<>();
                                        attributes.forEach((key, attr) -> {
                                            AttributeXMLConfiguration newAttributeConfiguration =
                                                    new AttributeXMLConfiguration(
                                                            attr.getXmlName(),
                                                            attr.getXmlAttributes(),
                                                            attr.getXmlRepresentation(),
                                                            attr.getElementRef(), //populate substitutionGroup with elementRef as per legacy format
                                                            Optional.<String>empty());
                                            newAttributes.put(key, newAttributeConfiguration);
                                        });
                                        return newAttributes;
                                    });

                    TypeXMLConfiguration newTypeXmlConfiguration = new TypeXMLConfiguration(
                            typeXMLConfiguration.getSubstitutionFor(),
                            typeXMLConfiguration.getSubstitutionGroup(),
                            typeXMLConfiguration.getXmlElementName(),
                            Optional.<String>empty(), //blank out XmlElementFullyQualifiedName as per legacy format
                            Optional.<Boolean>empty(), //blank out abstract as per legacy format
                            typeXMLConfiguration.getXmlAttributes(),
                            newAttributeXmlConfiguration,
                            typeXMLConfiguration.getEnumValues(),
                            Optional.empty()
                    );
                    newTypeConfigMap.put(modelSymbolId, newTypeXmlConfiguration);
                }
            });

            return new RosettaXMLConfiguration(newTypeConfigMap);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private RosettaXMLConfiguration getLegacyV1RosettaXMLConfiguration() {
        try (InputStream inputStream = configUrl.openStream()) {
            final RosettaXMLConfiguration config = RosettaXMLConfiguration.load(inputStream);
            Map<ModelSymbolId, TypeXMLConfiguration> newTypeConfigMap = new HashMap<>();

            config.getTypeConfigMap().forEach((modelSymbolId, typeXMLConfiguration) -> {
                if (!typeXMLConfiguration.getAbstract().orElse(false)) {
                    Optional<Map<String, AttributeXMLConfiguration>> newAttributeXmlConfiguration =
                            typeXMLConfiguration.getAttributes()
                                    .map(attributes -> {
                                        Map<String, AttributeXMLConfiguration> newAttributes = new HashMap<>();
                                        attributes.forEach((key, attr) -> {
                                            AttributeXMLConfiguration newAttributeConfiguration =
                                                    new AttributeXMLConfiguration(
                                                            attr.getXmlName(),
                                                            attr.getXmlAttributes(),
                                                            attr.getXmlRepresentation(),
                                                            attr.getElementRef(), //populate substitutionGroup with elementRef as per legacy format
                                                            Optional.<String>empty());
                                            newAttributes.put(key, newAttributeConfiguration);
                                        });
                                        return newAttributes;
                                    });

                    TypeXMLConfiguration newTypeXmlConfiguration = new TypeXMLConfiguration(
                            deriveLegacyV1SubstitutionFor(modelSymbolId, typeXMLConfiguration), //populate with type to substitute for as in v1 format
                            Optional.<String>empty(), //empty substitution group as in v1 format
                            typeXMLConfiguration.getXmlElementName(),
                            Optional.<String>empty(), //blank out XmlElementFullyQualifiedName as per legacy format
                            Optional.<Boolean>empty(), //blank out abstract as per legacy format
                            typeXMLConfiguration.getXmlAttributes(),
                            newAttributeXmlConfiguration,
                            typeXMLConfiguration.getEnumValues(),
                            Optional.empty()
                    );
                    newTypeConfigMap.put(modelSymbolId, newTypeXmlConfiguration);
                }
            });

            return new RosettaXMLConfiguration(newTypeConfigMap);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Optional<ModelSymbolId> deriveLegacyV1SubstitutionFor(
            ModelSymbolId modelSymbolId, TypeXMLConfiguration typeXMLConfiguration) {
        return typeXMLConfiguration.getSubstitutionGroup()
                .map(substitutionGroup -> {
                    String name = substitutionGroup.replaceAll(".*/(.*?)$", "$1");
                    name = name.substring(0, 1).toUpperCase() + name.substring(1);
                    DottedPath namespace = modelSymbolId.getNamespace();
                    return new ModelSymbolId(namespace, name);
                });
    }
}
