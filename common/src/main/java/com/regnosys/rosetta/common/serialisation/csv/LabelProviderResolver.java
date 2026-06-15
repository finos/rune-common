package com.regnosys.rosetta.common.serialisation.csv;

/*-
 * ==============
 * Rune Common
 * ==============
 * Copyright (C) 2018 - 2025 REGnosys
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

import com.rosetta.model.lib.annotations.RuneLabelProvider;
import com.rosetta.model.lib.functions.LabelProvider;
import com.rosetta.model.lib.functions.RosettaFunction;

/**
 * Resolves a {@link LabelProvider} from a Rune transform function class.
 *
 * <p>The Rune DSL generates a {@code @RuneLabelProvider} annotation on each transform
 * function class (projections, ingests, enrichments, reports). This resolver reads that
 * annotation and instantiates the referenced provider via its public no-arg constructor.
 *
 * <p>This class is unit-testable in isolation — it has no Jackson dependency.
 */
public class LabelProviderResolver {

    /**
     * Resolves a {@link LabelProvider} from the given transform function class.
     *
     * <p>Reads the {@code @RuneLabelProvider} annotation on {@code fn} and instantiates
     * the referenced provider class via its public no-arg constructor.
     *
     * @param fn the transform function class (must carry {@code @RuneLabelProvider})
     * @return the instantiated {@link LabelProvider}, or {@code null} if the annotation
     *         is absent
     * @throws IllegalStateException if the provider class cannot be instantiated
     */
    public static LabelProvider fromTransformFunction(Class<? extends RosettaFunction> fn) {
        RuneLabelProvider annotation = fn.getAnnotation(RuneLabelProvider.class);
        if (annotation == null) {
            return null;
        }
        Class<? extends LabelProvider> providerClass = annotation.labelProvider();
        try {
            return providerClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Failed to instantiate LabelProvider " + providerClass.getName()
                            + " for transform function " + fn.getName(), e);
        }
    }

    /**
     * Resolves a {@link LabelProvider} from a transform function class name, loading
     * the class via the supplied {@link ClassLoader}.
     *
     * <p>This is a convenience overload for use in pipeline contexts where the function
     * is identified by its fully-qualified class name (e.g. from
     * {@code PipelineModel.Transform.getFunction()}).
     *
     * @param functionClassName the fully-qualified name of the transform function class
     * @param classLoader       the class loader to use for loading the function class
     * @return the instantiated {@link LabelProvider}, or {@code null} if the function
     *         class carries no {@code @RuneLabelProvider} annotation
     * @throws IllegalArgumentException if the function class cannot be found or is not
     *                                  a {@link RosettaFunction}
     * @throws IllegalStateException    if the provider class cannot be instantiated
     */
    @SuppressWarnings("unchecked")
    public static LabelProvider fromTransformFunction(String functionClassName, ClassLoader classLoader) {
        Class<?> rawClass;
        try {
            rawClass = classLoader.loadClass(functionClassName);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(
                    "Transform function class not found: " + functionClassName, e);
        }
        if (!RosettaFunction.class.isAssignableFrom(rawClass)) {
            throw new IllegalArgumentException(
                    "Class " + functionClassName + " does not implement RosettaFunction");
        }
        return fromTransformFunction((Class<? extends RosettaFunction>) rawClass);
    }
}
