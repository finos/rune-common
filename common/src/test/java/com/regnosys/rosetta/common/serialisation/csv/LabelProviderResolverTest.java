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
import com.rosetta.model.lib.path.RosettaPath;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link LabelProviderResolver}.
 *
 * Uses inner stub classes to simulate the generated function/provider pattern
 * without any dependency on Jackson or the BNP model.
 */
public class LabelProviderResolverTest {

    // ---------------------------------------------------------------------------
    // Stubs
    // ---------------------------------------------------------------------------

    /** Minimal LabelProvider that returns a fixed label for "attr" and null otherwise. */
    public static class StubLabelProvider implements LabelProvider {
        @Override
        public String getLabel(RosettaPath path) {
            if ("attr".equals(path.buildPath())) {
                return "My Attribute Label";
            }
            return null;
        }
    }

    /** Stub RosettaFunction carrying @RuneLabelProvider pointing at StubLabelProvider. */
    @RuneLabelProvider(labelProvider = StubLabelProvider.class)
    public static class StubFunctionWithProvider implements RosettaFunction {}

    /** Stub RosettaFunction with NO @RuneLabelProvider annotation. */
    public static class StubFunctionWithoutProvider implements RosettaFunction {}

    // ---------------------------------------------------------------------------
    // Tests — Class<?> overload
    // ---------------------------------------------------------------------------

    @Test
    void fromTransformFunction_withAnnotation_returnsProvider() {
        LabelProvider provider = LabelProviderResolver.fromTransformFunction(StubFunctionWithProvider.class);

        assertNotNull(provider, "Expected a non-null LabelProvider when @RuneLabelProvider is present");
        assertInstanceOf(StubLabelProvider.class, provider);
    }

    @Test
    void fromTransformFunction_withAnnotation_labelLookupWorks() {
        LabelProvider provider = LabelProviderResolver.fromTransformFunction(StubFunctionWithProvider.class);

        assertNotNull(provider);
        assertEquals("My Attribute Label", provider.getLabel(RosettaPath.valueOf("attr")));
    }

    @Test
    void fromTransformFunction_withAnnotation_unlabelledAttributeReturnsNull() {
        LabelProvider provider = LabelProviderResolver.fromTransformFunction(StubFunctionWithProvider.class);

        assertNotNull(provider);
        assertNull(provider.getLabel(RosettaPath.valueOf("unlabelled")),
                "Unlabelled attribute should return null so callers can fall back to attribute name");
    }

    @Test
    void fromTransformFunction_withoutAnnotation_returnsNull() {
        LabelProvider provider = LabelProviderResolver.fromTransformFunction(StubFunctionWithoutProvider.class);

        assertNull(provider, "Expected null when @RuneLabelProvider annotation is absent");
    }

    // ---------------------------------------------------------------------------
    // Tests — String + ClassLoader overload
    // ---------------------------------------------------------------------------

    @Test
    void fromTransformFunction_byName_withAnnotation_returnsProvider() {
        LabelProvider provider = LabelProviderResolver.fromTransformFunction(
                StubFunctionWithProvider.class.getName(),
                Thread.currentThread().getContextClassLoader());

        assertNotNull(provider);
        assertInstanceOf(StubLabelProvider.class, provider);
    }

    @Test
    void fromTransformFunction_byName_withoutAnnotation_returnsNull() {
        LabelProvider provider = LabelProviderResolver.fromTransformFunction(
                StubFunctionWithoutProvider.class.getName(),
                Thread.currentThread().getContextClassLoader());

        assertNull(provider);
    }

    @Test
    void fromTransformFunction_byName_classNotFound_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () ->
                LabelProviderResolver.fromTransformFunction(
                        "com.example.NonExistentFunction",
                        Thread.currentThread().getContextClassLoader()));
    }

    @Test
    void fromTransformFunction_byName_notARosettaFunction_throwsIllegalArgument() {
        // String is a real class but does not implement RosettaFunction
        assertThrows(IllegalArgumentException.class, () ->
                LabelProviderResolver.fromTransformFunction(
                        String.class.getName(),
                        Thread.currentThread().getContextClassLoader()));
    }
}
