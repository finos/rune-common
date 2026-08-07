package com.regnosys.rosetta.common.transform;

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

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.annotations.RuneLabelProvider;
import com.rosetta.model.lib.functions.LabelProvider;
import com.rosetta.model.lib.functions.RosettaFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * Resolves a {@link LabelProvider} rooted at either of the two places the Rune DSL stamps a
 * {@code @RuneLabelProvider} annotation: a transform function class, or a model type's pojo
 * interface.
 *
 * <p>The two roots are not interchangeable. {@link #fromTransformFunction} resolves a provider
 * rooted at the function's <b>output</b> type — correct for a projection, wrong for an ingest,
 * whose output is the target model type rather than the serialised input. {@link #fromType}
 * resolves a provider rooted at the type itself, read from the {@code @RuneLabelProvider} the
 * DSL stamps on the generated pojo interface.
 *
 * <p>A {@code null} from {@link #fromType} is a normal, indefinite state, not an error: the DSL
 * emits a type-rooted provider only for a type carrying labels on its own (or inherited)
 * attributes, so a type whose labels all sit on nested paths has none, permanently.
 *
 * <p>The two entry points guard their own root, so neither can answer for the other's: the same
 * annotation type is stamped on both a transform function and a labelled type, so only the
 * {@link RosettaFunction} / {@link RosettaModelObject} bound distinguishes them. Without that,
 * a function class reaching {@link #fromType} would hand back its output-rooted provider through
 * the type-rooted path — the wrongly-rooted provider a caller's guard exists to reject.
 *
 * <p>This class is unit-testable in isolation — it has no Jackson dependency.
 */
public class LabelProviderResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(LabelProviderResolver.class);

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

    /**
     * Resolves a {@link LabelProvider} rooted at the given model type.
     *
     * <p>The {@code @RuneLabelProvider} annotation has neither {@code @Target} nor
     * {@code @Inherited}, and Java never inherits interface annotations onto implementing
     * classes. It is read from the pojo interface itself, so passing a builder or
     * {@code …Impl} class also works: the supertype hierarchy is searched breadth-first,
     * closest declaration wins, for the interface that declares it.
     *
     * <p>Only a {@link RosettaModelObject} can carry a type-rooted provider. This mirrors
     * {@link #fromTransformFunction}'s {@link RosettaFunction} bound, and is what stops a transform
     * function class — which carries the very same annotation, rooted at its output — from resolving
     * through this path; see the class javadoc.
     *
     * @param type the model type — a pojo interface, or a builder/{@code …Impl} class
     *             implementing or extending it
     * @return the instantiated {@link LabelProvider}, or {@code null} if {@code type} is not a
     *         {@link RosettaModelObject} or no type in its hierarchy carries
     *         {@code @RuneLabelProvider}. That is a normal, permanent state — not every type has
     *         labels on its own attributes.
     * @throws IllegalStateException if the provider class cannot be instantiated
     */
    public static LabelProvider fromType(Class<?> type) {
        if (type == null) {
            return null;
        }
        RuneLabelProvider annotation = findLabelProviderAnnotation(type);
        if (annotation == null) {
            return null;
        }
        if (!RosettaModelObject.class.isAssignableFrom(type)) {
            // Warn rather than stay silent: the annotation is present, so something intended this
            // class to supply labels, and returning null without saying so would leave a caller
            // wondering why its labels vanished. The likeliest cause is a transform function class
            // passed where a model type was expected.
            LOGGER.warn("Ignoring the @RuneLabelProvider on {}: it is not a RosettaModelObject, so it "
                            + "cannot carry a type-rooted LabelProvider. A transform function's provider is "
                            + "rooted at the function's output and must be resolved with "
                            + "fromTransformFunction instead.",
                    type.getName());
            return null;
        }
        Class<? extends LabelProvider> providerClass = annotation.labelProvider();
        try {
            return providerClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Failed to instantiate LabelProvider " + providerClass.getName()
                            + " for type " + type.getName(), e);
        }
    }

    /**
     * Breadth-first search of {@code type}'s supertype hierarchy — the type itself, then its
     * interfaces and superclass, then their interfaces and superclasses, and so on — for the
     * first declaration of {@code @RuneLabelProvider}.
     *
     * <p>Breadth-first from the most specific type matters for {@code Child extends Parent}
     * where both carry the annotation: a deep-path label declared on the outer type
     * deliberately overrides the inner type's own, so the most specific declaration must win.
     */
    private static RuneLabelProvider findLabelProviderAnnotation(Class<?> type) {
        Deque<Class<?>> toVisit = new ArrayDeque<>();
        Set<Class<?>> visited = new HashSet<>();
        toVisit.add(type);
        while (!toVisit.isEmpty()) {
            Class<?> current = toVisit.poll();
            if (!visited.add(current)) {
                continue;
            }
            RuneLabelProvider annotation = current.getAnnotation(RuneLabelProvider.class);
            if (annotation != null) {
                return annotation;
            }
            for (Class<?> iface : current.getInterfaces()) {
                toVisit.add(iface);
            }
            Class<?> superclass = current.getSuperclass();
            if (superclass != null) {
                toVisit.add(superclass);
            }
        }
        return null;
    }
}
