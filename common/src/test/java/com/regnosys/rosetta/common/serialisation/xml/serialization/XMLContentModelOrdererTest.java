package com.regnosys.rosetta.common.serialisation.xml.serialization;

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

import com.regnosys.rosetta.common.serialisation.xml.config.OccursMax;
import com.regnosys.rosetta.common.serialisation.xml.config.XMLContentModel;
import com.regnosys.rosetta.common.serialisation.xml.config.XMLContentModelNodeType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class XMLContentModelOrdererTest {

    // ---- helpers to build content models concisely ----

    private static XMLContentModel element(String name, int min, OccursMax max) {
        return new XMLContentModel(XMLContentModelNodeType.ELEMENT,
                Optional.of(name), Optional.empty(),
                Optional.of(Collections.singletonList(name)),
                Optional.of(min), Optional.of(max), Optional.empty());
    }

    private static XMLContentModel nested(String prop, String name, int min, OccursMax max) {
        return new XMLContentModel(XMLContentModelNodeType.ELEMENT,
                Optional.of(name), Optional.empty(),
                Optional.of(Arrays.asList(prop, name)),
                Optional.of(min), Optional.of(max), Optional.empty());
    }

    private static XMLContentModel group(XMLContentModelNodeType type, int min, OccursMax max, XMLContentModel... children) {
        return new XMLContentModel(type, Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.of(min), Optional.of(max), Optional.of(Arrays.asList(children)));
    }

    private static Set<String> set(String... s) {
        return new LinkedHashSet<>(Arrays.asList(s));
    }

    // ---- data-dependent ordering: the case a static reorder cannot handle ----

    @Test
    void dataDependentOrderPicksBranchOrderFromPresentData() {
        // CHOICE[ SEQ(x, a, b), SEQ(y, b, a) ]  -> order of a,b depends on x vs y
        XMLContentModel model = group(XMLContentModelNodeType.CHOICE, 1, OccursMax.of(1),
                group(XMLContentModelNodeType.SEQUENCE, 1, OccursMax.of(1),
                        element("x", 1, OccursMax.of(1)),
                        element("a", 1, OccursMax.of(1)),
                        element("b", 1, OccursMax.of(1))),
                group(XMLContentModelNodeType.SEQUENCE, 1, OccursMax.of(1),
                        element("y", 1, OccursMax.of(1)),
                        element("b", 1, OccursMax.of(1)),
                        element("a", 1, OccursMax.of(1))));
        XMLContentModelOrderer orderer = new XMLContentModelOrderer(model);

        assertEquals(Arrays.asList("x", "a", "b"), orderer.order(set("x", "a", "b")));
        assertEquals(Arrays.asList("y", "b", "a"), orderer.order(set("y", "a", "b")));
    }

    // ---- the FpML TradeIdentifier shape: partyReference before the virtual choice ----

    @Test
    void tradeIdentifierOrdersPartyReferenceBeforeVirtualChoice() {
        // CHOICE[ SEQ(issuer, tradeId),
        //         SEQ( SEQ(partyReference, accountReference?),
        //              CHOICE*( tradeIdentifierChoice.tradeId | tradeIdentifierChoice.versionedTradeId ) ) ]
        XMLContentModel model = group(XMLContentModelNodeType.CHOICE, 1, OccursMax.of(1),
                group(XMLContentModelNodeType.SEQUENCE, 1, OccursMax.of(1),
                        element("issuer", 1, OccursMax.of(1)),
                        element("tradeId", 1, OccursMax.of(1))),
                group(XMLContentModelNodeType.SEQUENCE, 1, OccursMax.of(1),
                        group(XMLContentModelNodeType.SEQUENCE, 1, OccursMax.of(1),
                                element("partyReference", 1, OccursMax.of(1)),
                                element("accountReference", 0, OccursMax.of(1))),
                        group(XMLContentModelNodeType.CHOICE, 0, OccursMax.unbounded(),
                                nested("tradeIdentifierChoice", "tradeId", 1, OccursMax.of(1)),
                                nested("tradeIdentifierChoice", "versionedTradeId", 1, OccursMax.of(1)))));
        XMLContentModelOrderer orderer = new XMLContentModelOrderer(model);

        assertEquals(set("issuer", "tradeId", "partyReference", "accountReference", "tradeIdentifierChoice"),
                orderer.getContentModelProperties());
        assertEquals(Arrays.asList("partyReference", "tradeIdentifierChoice"),
                orderer.order(set("partyReference", "tradeIdentifierChoice")));
        assertEquals(Arrays.asList("partyReference", "accountReference", "tradeIdentifierChoice"),
                orderer.order(set("partyReference", "accountReference", "tradeIdentifierChoice")));
        assertEquals(Arrays.asList("issuer", "tradeId"),
                orderer.order(set("issuer", "tradeId")));
    }

    // ---- safe fallback when the present set cannot be consumed by the model ----

    @Test
    void returnsNullWhenPresentSetCannotBeOrdered() {
        XMLContentModel model = group(XMLContentModelNodeType.SEQUENCE, 1, OccursMax.of(1),
                element("a", 1, OccursMax.of(1)),
                element("b", 1, OccursMax.of(1)));
        XMLContentModelOrderer orderer = new XMLContentModelOrderer(model);

        // "c" is not part of the model -> no full consumption -> null (caller keeps default order).
        assertNull(orderer.order(set("a", "c")));
    }

    @Test
    void emptyPresentSetYieldsEmptyOrder() {
        XMLContentModel model = group(XMLContentModelNodeType.SEQUENCE, 1, OccursMax.of(1),
                element("a", 0, OccursMax.of(1)));
        XMLContentModelOrderer orderer = new XMLContentModelOrderer(model);

        List<String> order = orderer.order(Collections.emptySet());
        assertEquals(Collections.emptyList(), order);
    }
}
