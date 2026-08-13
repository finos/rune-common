package com.regnosys.rosetta.common.serialisation;

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

import java.util.Objects;

/**
 * What sits at the <b>root</b> of the object graph a mapper will read or write: which {@link Side side}
 * of the transform it is, and — when the caller knows it — the model type itself.
 * <p>
 * Only the caller can supply this. A {@link TransformMapperFactory} is handed a
 * {@link TransformSerialization} and a function class, and neither of those says which side is being
 * serialized: {@code @Ingest}/{@code @Projection} may both be absent (a report, an enrichment, a model
 * generated before transform annotations) or both be present with the same format (a CSV-to-CSV
 * transform). The side is free information at the call site — a caller has already chosen to serialize
 * an input or an output — and guessing it from annotations is wrong in both directions.
 * <p>
 * Two things depend on it, both in {@link ClasspathTransformMapperFactory}'s labelled CSV handling —
 * {@code CSV_LABELLED}, and {@code CSV} whose configuration declares {@code headerStyle=LABEL}:
 * <ul>
 *   <li>{@link #getType()} supplies the type-rooted {@code @RuneLabelProvider} lookup, which is the only
 *       correct provider on an ingest read path.</li>
 *   <li>{@link #getSide()} decides whether the function's own provider may be used at all: that provider
 *       is rooted at the function's <b>output</b>, so it is valid on {@link Side#OUTPUT} and never on
 *       {@link Side#INPUT}.</li>
 * </ul>
 * <p>
 * Passing no {@code TransformRoot} at all (the two-argument
 * {@link TransformMapperFactory#create(TransformSerialization, Class)}, or an explicit {@code null})
 * means "the caller said nothing". Resolution then behaves exactly as it did before root context
 * existed: the function's provider is used if it has one, unguarded. Supplying a root is therefore
 * additive — it can only make resolution more accurate, never silently strip labels from a caller that
 * has not been updated.
 */
public final class TransformRoot {

    /** Which side of the transform the serialized object graph is. */
    public enum Side {
        /** The graph is the transform's input — what an {@code @Ingest} reads. */
        INPUT,
        /** The graph is the transform's output — what a projection, report or enrichment produces. */
        OUTPUT
    }

    private final Side side;
    private final Class<?> type;

    private TransformRoot(Side side, Class<?> type) {
        this.side = Objects.requireNonNull(side, "side must not be null; omit the TransformRoot entirely "
                + "if the side is unknown");
        this.type = type;
    }

    /** The transform's input side, rooted at {@code type} ({@code null} when the caller does not know it). */
    public static TransformRoot input(Class<?> type) {
        return new TransformRoot(Side.INPUT, type);
    }

    /** The transform's input side, with no known root type. */
    public static TransformRoot input() {
        return input(null);
    }

    /** The transform's output side, rooted at {@code type} ({@code null} when the caller does not know it). */
    public static TransformRoot output(Class<?> type) {
        return new TransformRoot(Side.OUTPUT, type);
    }

    /** The transform's output side, with no known root type. */
    public static TransformRoot output() {
        return output(null);
    }

    /** The general form, for a caller that computes the side rather than naming it. */
    public static TransformRoot of(Side side, Class<?> type) {
        return new TransformRoot(side, type);
    }

    /** Which side of the transform this root is. Never {@code null}. */
    public Side getSide() {
        return side;
    }

    /** The type at the root of the graph, or {@code null} when the caller does not know it. */
    public Class<?> getType() {
        return type;
    }

    /** Whether this root is the transform's output — where a function-rooted label provider is valid. */
    public boolean isOutput() {
        return side == Side.OUTPUT;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TransformRoot)) {
            return false;
        }
        TransformRoot other = (TransformRoot) o;
        return side == other.side && Objects.equals(type, other.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(side, type);
    }

    @Override
    public String toString() {
        return "TransformRoot{" + side + ", type=" + (type != null ? type.getName() : "<unknown>") + '}';
    }
}
