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

import com.regnosys.rosetta.common.transform.LabelProviderResolver;
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

    /** Stub pojo interface carrying @RuneLabelProvider directly — the common case. */
    @RuneLabelProvider(labelProvider = StubLabelProvider.class)
    public interface StubTypeWithProvider {}

    /** Stub pojo interface with NO @RuneLabelProvider annotation. */
    public interface StubTypeWithoutProvider {}

    /** Stub "…Impl" class implementing the annotated interface without declaring the annotation itself. */
    public static class StubTypeImpl implements StubTypeWithProvider {}

    /** Stub builder-shaped interface extending the annotated interface without declaring it itself. */
    public interface StubTypeBuilder extends StubTypeWithProvider {}

    /** Plain type with no @RuneLabelProvider anywhere in its hierarchy. */
    public static class StubPlainType {}

    /** LabelProvider distinguishable from {@link StubLabelProvider}, used by the Parent/Child test. */
    public static class ParentLabelProvider implements LabelProvider {
        @Override
        public String getLabel(RosettaPath path) {
            return "Parent Label";
        }
    }

    /** LabelProvider distinguishable from {@link ParentLabelProvider}, used by the Parent/Child test. */
    public static class ChildLabelProvider implements LabelProvider {
        @Override
        public String getLabel(RosettaPath path) {
            return "Child Label";
        }
    }

    /** Parent interface carrying its own provider. */
    @RuneLabelProvider(labelProvider = ParentLabelProvider.class)
    public interface StubParentType {}

    /**
     * Child interface extending Parent and carrying its own, different provider. A deep-path label
     * declared on the outer type deliberately overrides the inner type's own (producer §2.5), so when
     * both levels of a hierarchy are annotated, the most specific (Child's) declaration is the only
     * correct answer.
     */
    @RuneLabelProvider(labelProvider = ChildLabelProvider.class)
    public interface StubChildType extends StubParentType {}

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
    // Tests — fromType(Class<?>)
    // ---------------------------------------------------------------------------

    @Test
    void fromType_interfaceWithAnnotation_returnsProvider() {
        LabelProvider provider = LabelProviderResolver.fromType(StubTypeWithProvider.class);

        assertNotNull(provider, "Expected a non-null LabelProvider when @RuneLabelProvider is present");
        assertInstanceOf(StubLabelProvider.class, provider);
        assertEquals("My Attribute Label", provider.getLabel(RosettaPath.valueOf("attr")));
    }

    @Test
    void fromType_interfaceWithoutAnnotation_returnsNull() {
        LabelProvider provider = LabelProviderResolver.fromType(StubTypeWithoutProvider.class);

        assertNull(provider, "Expected null when @RuneLabelProvider annotation is absent");
    }

    @Test
    void fromType_implClassImplementingAnnotatedInterface_returnsProviderViaSearch() {
        LabelProvider provider = LabelProviderResolver.fromType(StubTypeImpl.class);

        assertNotNull(provider,
                "The annotation is not @Inherited, so an …Impl class must find it via the supertype search");
        assertInstanceOf(StubLabelProvider.class, provider);
    }

    @Test
    void fromType_builderInterfaceExtendingAnnotatedInterface_returnsProviderViaSearch() {
        LabelProvider provider = LabelProviderResolver.fromType(StubTypeBuilder.class);

        assertNotNull(provider,
                "A builder-shaped interface extending the annotated interface must find it via the search");
        assertInstanceOf(StubLabelProvider.class, provider);
    }

    @Test
    void fromType_childAndParentBothAnnotated_childsDeclarationWins() {
        LabelProvider provider = LabelProviderResolver.fromType(StubChildType.class);

        assertNotNull(provider);
        assertInstanceOf(ChildLabelProvider.class, provider,
                "Child's own declaration must win over Parent's — the most specific declaration is the "
                        + "only correct answer when both levels of a hierarchy are annotated (producer §2.5)");
    }

    @Test
    void fromType_noAnnotationAnywhereInHierarchy_returnsNull() {
        LabelProvider provider = LabelProviderResolver.fromType(StubPlainType.class);

        assertNull(provider);
    }

    // ---------------------------------------------------------------------------
    // Tests — fromType(String, ClassLoader)
    // ---------------------------------------------------------------------------

    @Test
    void fromType_byName_withAnnotation_returnsProvider() {
        LabelProvider provider = LabelProviderResolver.fromType(
                StubTypeWithProvider.class.getName(),
                Thread.currentThread().getContextClassLoader());

        assertNotNull(provider);
        assertInstanceOf(StubLabelProvider.class, provider);
    }

    @Test
    void fromType_byName_withoutAnnotation_returnsNull() {
        LabelProvider provider = LabelProviderResolver.fromType(
                StubTypeWithoutProvider.class.getName(),
                Thread.currentThread().getContextClassLoader());

        assertNull(provider);
    }

    @Test
    void fromType_byName_classNotFound_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () ->
                LabelProviderResolver.fromType(
                        "com.example.NonExistentType",
                        Thread.currentThread().getContextClassLoader()));
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
