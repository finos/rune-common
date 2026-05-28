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

import java.util.Optional;

/**
 * One ambiguous XML child element supplied to {@link XMLContentModelMatcher#route}.
 *
 * <p>{@link #fieldIndex} preserves the original ordinal index in the parent buffered field list so
 * that the matcher's assignment can be applied back to the original token stream regardless of
 * which fields were filtered out as XML attributes or pass-through.</p>
 */
final class RoutingInput {

    private final int fieldIndex;
    private final String xmlName;
    private final Namespace namespace;

    RoutingInput(int fieldIndex, String xmlName, Namespace namespace) {
        this.fieldIndex = fieldIndex;
        this.xmlName = xmlName;
        this.namespace = namespace;
    }

    int getFieldIndex() {
        return fieldIndex;
    }

    String getXmlName() {
        return xmlName;
    }

    Namespace getNamespace() {
        return namespace;
    }

    @Override
    public String toString() {
        return xmlName + "@" + fieldIndex;
    }

    /**
     * Namespace state for a routed XML field.
     *
     * <p>The distinction between {@link State#ABSENT} and {@link State#UNKNOWN} is deliberate.
     * Real XML parsed by {@code FromXmlParser} can tell us that an element has no namespace, and
     * that should still fail when the content model requires one. Replayed content from Jackson's
     * {@code TokenBuffer} has lost the original StAX namespace context, so it is unknown rather
     * than absent. In that case the matcher may fall back to local-name routing, while still
     * reporting ambiguity if namespace loss leaves multiple possible routes.</p>
     */
    static final class Namespace {
        private static final Namespace ABSENT = new Namespace(State.ABSENT, Optional.empty());
        private static final Namespace UNKNOWN = new Namespace(State.UNKNOWN, Optional.empty());

        private final State state;
        private final Optional<String> value;

        private Namespace(State state, Optional<String> value) {
            this.state = state;
            this.value = value;
        }

        static Namespace present(String namespace) {
            return new Namespace(State.PRESENT, Optional.of(namespace));
        }

        static Namespace absent() {
            return ABSENT;
        }

        static Namespace unknown() {
            return UNKNOWN;
        }

        boolean isUnknown() {
            return state == State.UNKNOWN;
        }

        Optional<String> getValue() {
            return value;
        }

        private enum State {
            PRESENT,
            ABSENT,
            UNKNOWN
        }
    }
}
