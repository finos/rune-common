package com.regnosys.rosetta.common.serialisation.xml.stax.read;

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
import com.rosetta.test.Snake;
import com.rosetta.test.SnakeDeadlinessEnum;
import com.rosetta.test.SnakeModel;
import com.rosetta.test.WrappedAnimalContainer;
import com.rosetta.test.WrappedAnimalContainerModel;
import com.rosetta.test.Zoo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link StaxReader} covering substitution-group deserialisation and
 * substitution-driven polymorphism (Step 4b of the StAX migration plan).
 *
 * <p>Mirrors {@link com.regnosys.rosetta.common.serialisation.xml.stax.write.StaxWriterSubstitutionTest}'s
 * fixtures for the read direction, plus the polymorphism fixtures from
 * {@code XmlSerialisationTest} (criterion 16 / issue 6: same local name, different namespace).
 */
public class StaxReaderSubstitutionTest {

    private static final String XML_CONFIG =
            "serialisation/xml/xml-config/extension-schema-xml-config.json";
    private static final String XML_TEST_RESOURCES = "serialisation/xml/";

    private URL configUrl;
    private StaxReader reader;

    @BeforeEach
    public void setUp() throws Exception {
        configUrl = Resources.getResource(XML_CONFIG);
        RosettaXMLConfiguration config;
        try (InputStream is = configUrl.openStream()) {
            config = RosettaXMLConfiguration.load(is);
        }
        reader = new StaxReader(config, getClass().getClassLoader());
    }

    // -------------------------------------------------------------------------
    // Substitution-group deserialisation (single + multi cardinality)
    // -------------------------------------------------------------------------

    @Test
    public void testSubstitutionGroupDeserialisation() throws Exception {
        String xml = Resources.toString(
                Resources.getResource(XML_TEST_RESOURCES + "expected/substitution-group.xml"),
                StandardCharsets.UTF_8);

        AnimalContainer actual = reader.read(xml, AnimalContainer.class);

        AnimalContainer expected = AnimalContainer.builder()
                .setAnimal(Goat.builder().setName("Goatee").build())
                .build();
        assertEquals(expected, actual);
    }

    @Test
    public void testMultiCardinalitySubstitutionGroupDeserialisation() throws Exception {
        String xml = Resources.toString(
                Resources.getResource(XML_TEST_RESOURCES + "expected/substitution-group-multi.xml"),
                StandardCharsets.UTF_8);

        Zoo actual = reader.read(xml, Zoo.class);

        Zoo expected = Zoo.builder()
                .addAnimal(Goat.builder().setName("Goatee").build())
                .addAnimal(Cow.builder().setName("Moomoo").build())
                .addAnimal(Shark.builder().setName("Jaws").build())
                .addAnimal(Salmon.builder().setName("Sashimi").build())
                .build();
        assertEquals(expected, actual);
    }

    // -------------------------------------------------------------------------
    // Legacy V2 config format
    // -------------------------------------------------------------------------

    /**
     * The legacy V2 config format blanks {@code xmlElementFullyQualifiedName}/{@code abstract}
     * at the type level, which breaks the transitive {@code fish -> shark/salmon} chain
     * (recursion is keyed off the intermediate type's own FQN) — the same limitation the
     * Jackson-era introspector has. Only goat/cow (direct animal substitutes) resolve.
     */
    @Test
    public void testMultiCardinalitySubstitutionGroupLegacyV2Deserialisation() throws Exception {
        String xml = Resources.toString(
                Resources.getResource(XML_TEST_RESOURCES + "expected/substitution-group-multi-legacy.xml"),
                StandardCharsets.UTF_8);

        StaxReader legacyReader = new StaxReader(getLegacyV2RosettaXMLConfiguration(), getClass().getClassLoader());
        Zoo actual = legacyReader.read(xml, Zoo.class);

        Zoo expected = Zoo.builder()
                .addAnimal(Goat.builder().setName("Goatee").build())
                .addAnimal(Cow.builder().setName("Moomoo").build())
                .build();
        assertEquals(expected, actual);
    }

    // -------------------------------------------------------------------------
    // "@type"-driven polymorphism (element-name-driven concrete type resolution)
    // -------------------------------------------------------------------------

    @Test
    public void testPolymorphicDeserialisation() throws Exception {
        String xml = Resources.toString(
                Resources.getResource(XML_TEST_RESOURCES + "input/polymorphic.xml"),
                StandardCharsets.UTF_8);

        AnimalContainer actual = reader.read(xml, AnimalContainer.class);

        AnimalContainer expected = AnimalContainer.builder()
                .setAnimal(Snake.builder().setName("Snakee")
                        .setSnakeModel(SnakeModel.builder()
                                .setDeadliness(SnakeDeadlinessEnum.DEADLY))).build();
        assertEquals(expected, actual);
    }

    @Test
    public void testPolymorphicReplacementDeserialisation() throws Exception {
        String xml = Resources.toString(
                Resources.getResource(XML_TEST_RESOURCES + "input/polymorphic-replacement.xml"),
                StandardCharsets.UTF_8);

        AnimalContainer actual = reader.read(xml, AnimalContainer.class);

        AnimalContainer expected = AnimalContainer.builder()
                .setAnimal(com.rosetta.extension.test.Snake.builder().setName("Snakee")
                        .setSnakeExtensionModel(com.rosetta.test.SnakeExtensionModel.builder()
                                .setDeadliness("MostlyHarmless"))
                ).build();
        assertEquals(expected, actual);
    }

    @Test
    public void testPolymorphicReplacementDeserialisationThroughVirtualWrapper() throws Exception {
        String xml = Resources.toString(
                Resources.getResource(XML_TEST_RESOURCES + "input/polymorphic-replacement-token-buffer-parser.xml"),
                StandardCharsets.UTF_8);

        WrappedAnimalContainer actual = reader.read(xml, WrappedAnimalContainer.class);

        WrappedAnimalContainer expected = WrappedAnimalContainer.builder()
                .setWrappedAnimalContainerModel(WrappedAnimalContainerModel.builder()
                        .setAnimal(com.rosetta.extension.test.Snake.builder().setName("Snakee")
                                .setSnakeExtensionModel(com.rosetta.test.SnakeExtensionModel.builder()
                                        .setDeadliness("MostlyHarmless"))
                        )).build();
        assertEquals(expected, actual);
    }

    // -------------------------------------------------------------------------
    // Criterion 16 — namespace-aware substitution resolves a local-name collision
    // -------------------------------------------------------------------------

    /**
     * "camel" is a substitution-group member in BOTH the base schema
     * ({@code urn:my.schema/camel -> com.rosetta.test.Camel}) and the extension schema
     * ({@code urn:my.extension/camel -> com.rosetta.extension.test.Camel}). Without
     * namespace-aware resolution, "first wins" would silently pick the wrong concrete type
     * (issue 6). {@link SubstitutionResolver} resolves by exact (namespace, local name) match.
     */
    @Test
    public void testNamespaceAwareSubstitutionResolvesLocalNameCollision() throws Exception {
        String xml = Resources.toString(
                Resources.getResource(XML_TEST_RESOURCES + "input/polymorphic-replacement-ambiguous-choice.xml"),
                StandardCharsets.UTF_8);

        WrappedAnimalContainer actual = reader.read(xml, WrappedAnimalContainer.class);

        WrappedAnimalContainer expected = WrappedAnimalContainer.builder()
                .setWrappedAnimalContainerModel(WrappedAnimalContainerModel.builder()
                        .setAnimal(com.rosetta.extension.test.Camel.builder().setName("Humpee")
                                .setHumps(2))
                ).build();
        assertEquals(expected, actual);
    }

    // -------------------------------------------------------------------------
    // Legacy config builder — replicated from StaxWriterSubstitutionTest
    // -------------------------------------------------------------------------

    private RosettaXMLConfiguration getLegacyV2RosettaXMLConfiguration() throws IOException {
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
                                                            attr.getElementRef(),
                                                            Optional.<String>empty());
                                            newAttributes.put(key, newAttributeConfiguration);
                                        });
                                        return newAttributes;
                                    });

                    TypeXMLConfiguration newTypeXmlConfiguration = new TypeXMLConfiguration(
                            typeXMLConfiguration.getSubstitutionFor(),
                            typeXMLConfiguration.getSubstitutionGroup(),
                            typeXMLConfiguration.getXmlElementName(),
                            Optional.<String>empty(),
                            Optional.<Boolean>empty(),
                            typeXMLConfiguration.getXmlAttributes(),
                            newAttributeXmlConfiguration,
                            typeXMLConfiguration.getEnumValues(),
                            Optional.empty()
                    );
                    newTypeConfigMap.put(modelSymbolId, newTypeXmlConfiguration);
                }
            });

            return new RosettaXMLConfiguration(newTypeConfigMap);
        }
    }
}
