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
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable value type representing the {@code maxOccurs} bound of an XML content-model node.
 *
 * <p>Supports either a finite non-negative integer or the special "unbounded" value.
 * Accepts JSON input as either a number ({@code 3}) or the string {@code "unbounded"}.</p>
 */
public final class OccursMax {

    public static final String UNBOUNDED_TOKEN = "unbounded";

    private static final OccursMax UNBOUNDED = new OccursMax(Optional.empty());

    private final Optional<Integer> value;

    private OccursMax(Optional<Integer> value) {
        this.value = value;
    }

    public static OccursMax unbounded() {
        return UNBOUNDED;
    }

    public static OccursMax of(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("maxOccurs must be non-negative, was " + value);
        }
        return new OccursMax(Optional.of(value));
    }

    @JsonCreator
    public static OccursMax fromJson(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number) {
            return of(((Number) raw).intValue());
        }
        if (raw instanceof String) {
            String text = ((String) raw).trim();
            if (UNBOUNDED_TOKEN.equalsIgnoreCase(text)) {
                return unbounded();
            }
            try {
                return of(Integer.parseInt(text));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid maxOccurs value: '" + raw + "'");
            }
        }
        throw new IllegalArgumentException("Invalid maxOccurs value: " + raw);
    }

    @JsonValue
    public Object toJson() {
        return value.<Object>map(v -> v).orElse(UNBOUNDED_TOKEN);
    }

    public Optional<Integer> getValue() {
        return value;
    }

    public boolean isUnbounded() {
        return !value.isPresent();
    }

    /**
     * Resolves this {@code maxOccurs} bound against the remaining input length.
     * Returns the upper bound of how many times a node may be repeated.
     */
    public int boundedValue(int inputRemaining) {
        if (isUnbounded()) {
            return inputRemaining;
        }
        return value.orElse(1);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof OccursMax)) {
            return false;
        }
        OccursMax other = (OccursMax) obj;
        return Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value.map(String::valueOf).orElse(UNBOUNDED_TOKEN);
    }
}
