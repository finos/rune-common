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

/**
 * Reproduces a bug in {@link XMLContentModelMatcher#repeat}: a {@code SEQUENCE} node with
 * {@code minOccurs=1} whose only child is an already-optional {@code CHOICE} node
 * ({@code minOccurs=0}) is incorrectly rejected as {@code NO_MATCH} when none of the choice's
 * branches are present in the input, even though this is a structurally valid, fully-optional
 * content model (the XML Schema equivalent of
 * {@code <xs:sequence><xs:choice minOccurs="0">...</xs:choice></xs:sequence>}).
 *
 * <p>This is the exact shape generated for Rosetta types such as {@code TradeIdentifier}, whose
 * top-level content model is a mandatory {@code SEQUENCE} wrapping a single optional
 * {@code CHOICE} of {@code issuer+tradeId} or {@code partyReference(+accountReference,tradeId*)}.
 * When an instance of such a type (or a subtype with additional, unrelated appended fields) has
 * none of the choice's own elements present, {@link XMLContentModelMatcher#route} returns
 * {@code NO_MATCH} instead of {@code SUCCESS}.</p>
 */
class XMLContentModelMatcherOptionalChoiceInSequenceTest {

    @Test
    void sequenceWithSoleOptionalChoiceIsSatisfiedByZeroMatchingInputs() {
        // SEQUENCE(minOccurs=1, maxOccurs=1) -> [ CHOICE(minOccurs=0, maxOccurs=1) -> [fieldA, fieldB] ]
        XMLContentModel model = sequenceOnce(
                optionalChoice(
                        element("fieldA", "fieldA"),
                        element("fieldB", "fieldB")));

        // No input elements at all: the choice legitimately matches zero times (it is optional),
        // so the enclosing sequence's single, empty-content occurrence should satisfy its own
        // minOccurs=1 and the whole document should be reported as a successful (empty) match.
        XMLContentModelMatcher.RoutingResult result = XMLContentModelMatcher.route(model, Collections.emptyList());

        assertEquals(XMLContentModelMatcher.RoutingResult.Status.SUCCESS, result.getStatus());
    }

    @Test
    void sequenceWithSoleOptionalChoiceStillRoutesWhenAChoiceBranchIsPresent() {
        // Sanity check: the same content model correctly routes when one of the choice's own
        // elements actually is present, confirming the bug is specific to the zero-element case.
        XMLContentModel model = sequenceOnce(
                optionalChoice(
                        element("fieldA", "fieldA"),
                        element("fieldB", "fieldB")));

        XMLContentModelMatcher.RoutingResult result = XMLContentModelMatcher.route(model,
                Collections.singletonList(input("fieldA")));

        assertEquals(XMLContentModelMatcher.RoutingResult.Status.SUCCESS, result.getStatus());
        assertEquals(Collections.singletonList("fieldA"), result.getPathByIndex().get(0));
    }

    private static RoutingInput input(String xmlName) {
        return new RoutingInput(0, xmlName, RoutingInput.Namespace.unknown());
    }

    private static XMLContentModel element(String xmlName, String path) {
        return new XMLContentModel(
                XMLContentModelNodeType.ELEMENT,
                Optional.of(xmlName),
                Optional.empty(),
                Optional.of(Collections.singletonList(path)),
                Optional.of(1),
                Optional.of(OccursMax.of(1)),
                Optional.empty());
    }

    private static XMLContentModel optionalChoice(XMLContentModel... children) {
        return new XMLContentModel(
                XMLContentModelNodeType.CHOICE,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(0),
                Optional.of(OccursMax.of(1)),
                Optional.of(Arrays.asList(children)));
    }

    private static XMLContentModel sequenceOnce(XMLContentModel... children) {
        return new XMLContentModel(
                XMLContentModelNodeType.SEQUENCE,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(1),
                Optional.of(OccursMax.of(1)),
                Optional.of(Arrays.asList(children)));
    }
}
