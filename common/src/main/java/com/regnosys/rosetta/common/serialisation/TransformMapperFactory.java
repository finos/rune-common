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
     * Builds the mapper for the given serialization. The function class is supplied because some
     * construction concerns need it: resolving the serialization config and model types against the
     * model's classloader, and instantiating the {@code @RuneLabelProvider} for the
     * {@code CSV_LABELLED} format. May be {@code null} when no function context exists.
     */
    ObjectMapper create(TransformSerialization serialization, Class<?> functionClass);

    /**
     * Builds the pretty-printing writer for the given serialization — the output-side counterpart of
     * {@link #create(TransformSerialization, Class)}.
     */
    default ObjectWriter createWriter(TransformSerialization serialization, Class<?> functionClass) {
        return create(serialization, functionClass).writerWithDefaultPrettyPrinter();
    }

    /**
     * Builds the mapper for the given serialization, told what sits at the <b>root</b> of the object
     * graph it will read or write — which side of the transform this is, and the root type when the
     * caller knows it. See {@link TransformRoot}, which explains why only the caller can supply this.
     * <p>
     * It lets the {@code CSV_LABELLED} format resolve a type-rooted {@code LabelProvider} — the only
     * correct provider on an ingest read path — and stops a function-rooted provider being used on the
     * input side, where it is rooted at the wrong type. {@code null} means "the caller said nothing" and
     * preserves the function-derived behaviour of {@link #create(TransformSerialization, Class)}.
     * <p>
     * The default implementation ignores {@code root}. Implementors that extend
     * {@link ClasspathTransformMapperFactory} inherit a real implementation; a factory written from
     * scratch must override this to get type-rooted labels.
     * <p>
     * There is deliberately no {@code createWriter} counterpart. Nothing needs one yet, and its whole
     * body would be {@code create(serialization, functionClass, root).writerWithDefaultPrettyPrinter()},
     * which a caller can write. Add the overload in the change that first requires it.
     */
    default ObjectMapper create(TransformSerialization serialization, Class<?> functionClass, TransformRoot root) {
        return create(serialization, functionClass);
    }
}
