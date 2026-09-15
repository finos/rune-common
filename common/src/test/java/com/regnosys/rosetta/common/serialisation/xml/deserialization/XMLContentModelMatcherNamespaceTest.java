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

import com.regnosys.rosetta.common.serialisation.xml.config.OccursMax;
import com.regnosys.rosetta.common.serialisation.xml.config.XMLContentModel;
import com.regnosys.rosetta.common.serialisation.xml.config.XMLContentModelNodeType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XMLContentModelMatcherNamespaceTest {

    private static final String NS_A = "urn:test:a";
    private static final String NS_B = "urn:test:b";

    @Test
    void requiredNamespaceMatchesWhenInputNamespaceIsPresentAndEqual() {
        XMLContentModel model = element("tradeId", NS_A, "tradeId");

        XMLContentModelMatcher.RoutingResult result = XMLContentModelMatcher.route(model, Collections.singletonList(
                input("tradeId", RoutingInput.Namespace.present(NS_A))));

        assertEquals(XMLContentModelMatcher.RoutingResult.Status.SUCCESS, result.getStatus());
        assertEquals(Collections.singletonList("tradeId"), result.getPathByIndex().get(0));
    }

    @Test
    void requiredNamespaceRejectsKnownDifferentInputNamespace() {
        XMLContentModel model = element("tradeId", NS_A, "tradeId");

        XMLContentModelMatcher.RoutingResult result = XMLContentModelMatcher.route(model, Collections.singletonList(
                input("tradeId", RoutingInput.Namespace.present(NS_B))));

        assertEquals(XMLContentModelMatcher.RoutingResult.Status.NO_MATCH, result.getStatus());
    }

    @Test
    void requiredNamespaceRejectsKnownAbsentInputNamespace() {
        XMLContentModel model = element("tradeId", NS_A, "tradeId");

        XMLContentModelMatcher.RoutingResult result = XMLContentModelMatcher.route(model, Collections.singletonList(
                input("tradeId", RoutingInput.Namespace.absent())));

        assertEquals(XMLContentModelMatcher.RoutingResult.Status.NO_MATCH, result.getStatus());
    }

    @Test
    void requiredNamespaceAllowsUnknownInputNamespaceFromTokenReplay() {
        XMLContentModel model = element("tradeId", NS_A, "tradeId");

        XMLContentModelMatcher.RoutingResult result = XMLContentModelMatcher.route(model, Collections.singletonList(
                input("tradeId", RoutingInput.Namespace.unknown())));

        assertEquals(XMLContentModelMatcher.RoutingResult.Status.SUCCESS, result.getStatus());
        assertEquals(Collections.singletonList("tradeId"), result.getPathByIndex().get(0));
    }

    @Test
    void unknownInputNamespaceStillReportsAmbiguousNamespaceQualifiedRoutes() {
        XMLContentModel model = choice(
                element("tradeId", NS_A, "aTradeId"),
                element("tradeId", NS_B, "bTradeId"));

        XMLContentModelMatcher.RoutingResult result = XMLContentModelMatcher.route(model, Collections.singletonList(
                input("tradeId", RoutingInput.Namespace.unknown())));

        assertEquals(XMLContentModelMatcher.RoutingResult.Status.AMBIGUOUS, result.getStatus());
        assertEquals(2, result.getCandidatePaths().size());
        assertTrue(result.getCandidatePaths().stream()
                .anyMatch(paths -> paths.contains(Collections.singletonList("aTradeId"))));
        assertTrue(result.getCandidatePaths().stream()
                .anyMatch(paths -> paths.contains(Collections.singletonList("bTradeId"))));
    }

    private static RoutingInput input(String xmlName, RoutingInput.Namespace namespace) {
        return new RoutingInput(0, xmlName, namespace);
    }

    private static XMLContentModel element(String xmlName, String namespace, String path) {
        return new XMLContentModel(
                XMLContentModelNodeType.ELEMENT,
                Optional.of(xmlName),
                Optional.of(namespace),
                Optional.of(Collections.singletonList(path)),
                Optional.of(1),
                Optional.of(OccursMax.of(1)),
                Optional.empty());
    }

    private static XMLContentModel choice(XMLContentModel... children) {
        return new XMLContentModel(
                XMLContentModelNodeType.CHOICE,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(1),
                Optional.of(OccursMax.of(1)),
                Optional.of(Arrays.asList(children)));
    }
}
