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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * Constructs the Jackson mapper for a resolved {@link TransformSerialization}. This is the
 * <em>construction</em> half of transform serialization — the counterpart of the pure decision in
 * {@link TransformSerializationResolver} — and the seam behind which all {@link ClassLoader} use lives:
 * a constructed mapper may hold references into the model's classloader (an XML config, a
 * {@code LabelProvider} instance, resolved model types), so implementations own where that construction
 * happens and how the result is cached.
 * <p>
 * On the classpath (tests, model builds) use {@link ClasspathTransformMapperFactory}. Runtimes that load
 * models in isolated, disposable classloaders must implement this on the component that owns the model
 * classloader and its lifecycle, so constructed mappers are cached alongside it and die with it —
 * building through a classpath-style factory there would leak the classloader.
 */
public interface TransformMapperFactory {

    /**
     * Builds the mapper for the given serialization.
     * <p>
     * The function class is supplied because some construction concerns need it: resolving the
     * serialization config and model types against the model's classloader, and instantiating the
     * {@code @RuneLabelProvider} for a labelled CSV format. May be {@code null} when no function context
     * exists.
     * <p>
     * The {@code root} says what sits at the <b>root</b> of the object graph the mapper will read or
     * write — which side of the transform this is, and the root type when the caller knows it. See
     * {@link TransformRoot}, which explains why only the caller can supply this. It lets a labelled CSV
     * format ({@code CSV_LABELLED}, or {@code CSV} configured with {@code headerStyle=LABEL}) resolve a
     * type-rooted {@code LabelProvider} — the only correct provider on an ingest read path — and stops a
     * function-rooted provider being used on the input side, where it is rooted at the wrong type.
     * <p>
     * {@code root} is a required parameter but {@code null} is a legal value, meaning "the caller said
     * nothing": resolution then behaves as it did before root context existed, and the function's own
     * provider is used unguarded. Pass {@code null} only where there genuinely is no side — a JSON
     * mapper, a null function class. A caller that knows which side it is serializing should say so,
     * because the unguarded fallback is wrong on the input side.
     */
    ObjectMapper create(TransformSerialization serialization, Class<?> functionClass, TransformRoot root);

    /**
     * Builds the pretty-printing writer for the given serialization — the {@link ObjectWriter}
     * counterpart of {@link #create(TransformSerialization, Class, TransformRoot)}, and subject to the
     * same {@code root} contract.
     */
    default ObjectWriter createWriter(TransformSerialization serialization, Class<?> functionClass, TransformRoot root) {
        return create(serialization, functionClass, root).writerWithDefaultPrettyPrinter();
    }
}
