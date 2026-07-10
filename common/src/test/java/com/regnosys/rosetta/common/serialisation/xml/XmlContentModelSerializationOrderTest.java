package com.regnosys.rosetta.common.serialisation.xml;

/*-
 * ==============
 * Rune Common
 * ==============
 * Copyright (C) 2018 - 2026 REGnosys
 * ==============
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * ==============
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.io.Resources;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapperCreator;
import com.rosetta.test.FpmlReference;
import com.rosetta.test.FpmlTextValue;
import com.rosetta.test.FpmlTradeIdentifier;
import com.rosetta.test.FpmlTradeIdentifierChoice;
import com.rosetta.test.SchemaLocationContainer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * End-to-end round-trip test for content-model-ordered XML serialization.
 *
 * <p>The XSD content model for {@code FpmlTradeIdentifier} (branch 2) requires:
 * {@code partyReference, accountReference?, (tradeId | versionedTradeId)+}, where the choice is
 * carried by the VIRTUAL property {@code tradeIdentifierChoice}.</p>
 *
 * <p>The deserializer enforces this order via {@code XMLContentModelDisambiguatingDeserializer}.
 * Without content-model-aware serialization the serializer emits child elements in bean-property
 * order: the VIRTUAL {@code tradeIdentifierChoice} is unwrapped to the FRONT, producing
 * {@code versionedTradeId, partyReference} — the wrong (non-XSD) order whose re-deserialisation
 * fails. This test asserts the serializer now follows the content model and the output round-trips.</p>
 */
public class XmlContentModelSerializationOrderTest {

    private final ObjectMapper xmlMapper;

    public XmlContentModelSerializationOrderTest() throws IOException {
        try (InputStream inputStream = Resources.getResource(
                "serialisation/xml/xml-config/content-model-xml-config.json").openStream()) {
            xmlMapper = RosettaObjectMapperCreator.forXML(inputStream).create();
        }
    }

    @Test
    public void serializerEmitsContentModelOrderAndRoundTrips() throws IOException {
        // A valid object: partyReference + a versionedTradeId carried by the virtual choice.
        FpmlTradeIdentifier object = FpmlTradeIdentifier.builder()
                .setPartyReference(FpmlReference.builder().setHref("party-1").build())
                .addTradeIdentifierChoice(FpmlTradeIdentifierChoice.builder()
                        .setVersionedTradeId(FpmlTextValue.builder().setValue("V-1").build())
                        .build())
                .build();

        ObjectWriter writer = xmlMapper.writerWithDefaultPrettyPrinter();
        String xml = writer.writeValueAsString(object);
        System.out.println("=== SERIALISED ===\n" + xml);

        int vtid = xml.indexOf("versionedTradeId");
        int pref = xml.indexOf("partyReference");
        assertTrue(vtid >= 0 && pref >= 0, "both elements should be present");

        // The serializer follows the XSD content model, so partyReference is emitted BEFORE
        // versionedTradeId.
        assertTrue(pref < vtid,
                "Expected partyReference before versionedTradeId (XSD order), got:\n" + xml);

        // And the serializer's own output round-trips cleanly through the strict deserializer.
        try {
            FpmlTradeIdentifier roundTripped = xmlMapper.readValue(xml, FpmlTradeIdentifier.class);
            assertEquals(object, roundTripped, "round-tripped object should equal the original");
            System.out.println("Round-trip succeeded and matched the original object.");
        } catch (Exception e) {
            fail("Re-deserialisation of serializer output failed: " + e.getMessage());
        }
    }

    @Test
    public void schemaLocationIsEmittedOnlyOnTheRootNotNestedContentModelElements() throws IOException {
        // A root containing a nested type that itself has a content model (and therefore a
        // content-model orderer). The xsi:schemaLocation attribute must be written on the root
        // element only, never on the nested content-model element.
        SchemaLocationContainer container = SchemaLocationContainer.builder()
                .setTradeIdentifier(FpmlTradeIdentifier.builder()
                        .setPartyReference(FpmlReference.builder().setHref("party-1").build())
                        .addTradeIdentifierChoice(FpmlTradeIdentifierChoice.builder()
                                .setVersionedTradeId(FpmlTextValue.builder().setValue("V-1").build())
                                .build())
                        .build())
                .build();

        String xml = xmlMapper.writerWithDefaultPrettyPrinter()
                .withAttribute("schemaLocation", "urn:my.schema ../schema/schema.xsd")
                .writeValueAsString(container);
        System.out.println("=== SERIALISED (with schemaLocation) ===\n" + xml);

        Matcher matcher = Pattern.compile("xsi:schemaLocation").matcher(xml);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        assertEquals(1, count, "xsi:schemaLocation must appear exactly once (root only):\n" + xml);
    }
}
