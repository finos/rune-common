package com.regnosys.rosetta.common.serialisation.xml.stax.introspect;

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

import com.regnosys.rosetta.common.serialisation.xml.config.AttributeXMLConfiguration;
import com.regnosys.rosetta.common.serialisation.xml.config.AttributeXMLRepresentation;
import com.regnosys.rosetta.common.serialisation.xml.config.RosettaXMLConfiguration;
import com.regnosys.rosetta.common.serialisation.xml.config.TypeXMLConfiguration;
import com.regnosys.rosetta.common.serialisation.xml.config.XMLContentModel;
import com.rosetta.model.lib.ModelSymbolId;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Multi;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RosettaIgnore;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.annotations.RuneIgnore;
import com.rosetta.util.DottedPath;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Builds a {@link TypeBinding} for a Rune-generated type.
 *
 * <p>Replaces Jackson's {@code BeanDescription} / {@code AnnotationIntrospector} as the
 * structural foundation for the StAX binder (Section&nbsp;1 of the XML mapper migration).
 * Given a Rune type and an {@link RosettaXMLConfiguration}, it returns the type's
 * attributes in bean declaration order with their XML binding metadata resolved.
 *
 * <h2>Declaration order</h2>
 * Attributes are returned in bean declaration order: supertype attributes always
 * precede subtype attributes. Within each type level, declaration order is derived
 * from {@link Class#getDeclaredFields()}, which the JVM spec guarantees follows
 * source order (unlike {@link Class#getDeclaredMethods()}, which does not on Java 9+).
 *
 * <h2>Caching</h2>
 * Introspecting a type is pure reflection ({@link Class#getDeclaredFields()},
 * {@link Class#getMethods()} per attribute) and its result depends only on the type and the
 * config, both immutable for the lifetime of a read or write. The binder asks for the same
 * handful of types once per XML element, so results are memoised per (config, type); without
 * this, reflection dominates the cost of every document.
 *
 * <p>The cache is concurrent because an introspector is owned by a {@code StaxReader} /
 * {@code StaxWriter}, which in turn is owned by one mapper instance — and a mapper (in particular
 * the {@code ObjectMapper} facade, whose contract promises it) may be shared across threads.
 * {@link TypeBinding} itself is immutable, so publishing a completed binding is safe; a race
 * merely recomputes one.
 */
public class RuneTypeIntrospector {

    /**
     * Memoised bindings for {@link #cachedConfig}. The hot path is one volatile read plus one map
     * lookup — no key allocation, and no {@code equals} on the config itself
     * ({@link RosettaXMLConfiguration} overrides it, so hashing by value would deep-compare a
     * structure with thousands of entries on every element).
     */
    private final ConcurrentMap<Class<?>, TypeBinding> bindings =
            new ConcurrentHashMap<Class<?>, TypeBinding>();

    /**
     * The config {@link #bindings} was populated for, compared by reference. Every caller reuses a
     * single config instance for its lifetime, so this is set once; if a caller does pass a
     * different config, the cache is dropped and rebuilt rather than returning a stale binding.
     */
    private volatile RosettaXMLConfiguration cachedConfig;

    public TypeBinding introspect(Class<?> type, RosettaXMLConfiguration config) {
        if (config != cachedConfig) {
            resetCacheFor(config);
        }
        TypeBinding cached = bindings.get(type);
        if (cached != null) {
            return cached;
        }
        TypeBinding binding = doIntrospect(type, config);
        TypeBinding raced = bindings.putIfAbsent(type, binding);
        return raced != null ? raced : binding;
    }

    private synchronized void resetCacheFor(RosettaXMLConfiguration config) {
        if (config == cachedConfig) {
            return;
        }
        bindings.clear();
        cachedConfig = config;
    }

    private TypeBinding doIntrospect(Class<?> type, RosettaXMLConfiguration config) {
        Class<?> builderClass = getBuilderClass(type);

        List<Class<?>> builderHierarchy = collectBuilderHierarchy(builderClass);
        List<TypeXMLConfiguration> typeConfigs = collectTypeConfigs(builderClass, config);

        Optional<TypeXMLConfiguration> primaryConfig = typeConfigs.isEmpty()
                ? Optional.<TypeXMLConfiguration>empty()
                : Optional.of(typeConfigs.get(0));

        List<AttributeBinding> attributes = collectAttributes(builderClass, builderHierarchy, typeConfigs);

        String logicalTypeName = getLogicalTypeName(type);
        String xmlElementName = resolveXmlElementName(primaryConfig, logicalTypeName);
        Optional<String> xmlElementNamespace = resolveXmlNamespace(primaryConfig);
        Map<String, String> xmlConstantAttrs = resolveXmlConstantAttributes(typeConfigs);
        Optional<XMLContentModel> contentModel = primaryConfig.flatMap(TypeXMLConfiguration::getContentModel);
        boolean isAbstract = primaryConfig.flatMap(TypeXMLConfiguration::getAbstract).orElse(false);

        return new TypeBinding(type, builderClass, attributes, xmlElementName,
                xmlElementNamespace, xmlConstantAttrs, contentModel, isAbstract);
    }

    // -------------------------------------------------------------------------
    // Builder class discovery
    // -------------------------------------------------------------------------

    private Class<?> getBuilderClass(Class<?> type) {
        RuneDataType runeAnn = type.getAnnotation(RuneDataType.class);
        if (runeAnn != null) {
            return runeAnn.builder();
        }
        RosettaDataType rosettaAnn = type.getAnnotation(RosettaDataType.class);
        if (rosettaAnn != null) {
            return rosettaAnn.builder();
        }
        throw new IllegalArgumentException(
                "Type " + type.getName() + " has neither @RuneDataType nor @RosettaDataType");
    }

    private String getLogicalTypeName(Class<?> type) {
        RuneDataType runeAnn = type.getAnnotation(RuneDataType.class);
        if (runeAnn != null && !runeAnn.value().isEmpty()) {
            return runeAnn.value();
        }
        RosettaDataType rosettaAnn = type.getAnnotation(RosettaDataType.class);
        if (rosettaAnn != null && !rosettaAnn.value().isEmpty()) {
            return rosettaAnn.value();
        }
        return type.getSimpleName();
    }

    // -------------------------------------------------------------------------
    // Builder class hierarchy
    // -------------------------------------------------------------------------

    /**
     * Returns the builder impl class hierarchy in root-to-leaf order.
     * Only classes whose declaring Rune interface carries {@code @RuneDataType} or
     * {@code @RosettaDataType} are included, so generated base classes are traversed
     * but unrelated superclasses (e.g., {@code Object}) are silently skipped.
     */
    private List<Class<?>> collectBuilderHierarchy(Class<?> builderClass) {
        List<Class<?>> result = new ArrayList<>();
        Class<?> current = builderClass;
        while (current != null && !Object.class.equals(current)) {
            Class<?> declaringClass = current.getDeclaringClass();
            if (declaringClass != null && isRuneType(declaringClass)) {
                result.add(current);
            }
            current = current.getSuperclass();
        }
        Collections.reverse(result);
        return result;
    }

    private boolean isRuneType(Class<?> type) {
        return type.isAnnotationPresent(RuneDataType.class)
                || type.isAnnotationPresent(RosettaDataType.class);
    }

    // -------------------------------------------------------------------------
    // Type config lookup
    // -------------------------------------------------------------------------

    /**
     * Returns {@link TypeXMLConfiguration} objects for the full type hierarchy in
     * most-derived-first order.  Uses the builder class hierarchy to discover the
     * corresponding Rune types, so no Jackson {@code AnnotatedClass} is needed.
     */
    private List<TypeXMLConfiguration> collectTypeConfigs(
            Class<?> builderClass, RosettaXMLConfiguration config) {
        List<TypeXMLConfiguration> result = new ArrayList<>();
        Class<?> current = builderClass;
        while (current != null && !Object.class.equals(current)) {
            Class<?> declaringClass = current.getDeclaringClass();
            if (declaringClass != null && isRuneType(declaringClass)) {
                String logicalName = getLogicalTypeName(declaringClass);
                String namespace = declaringClass.getPackage().getName();
                ModelSymbolId symbolId = new ModelSymbolId(DottedPath.splitOnDots(namespace), logicalName);
                config.getConfigurationForType(symbolId).ifPresent(result::add);
            }
            current = current.getSuperclass();
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Attribute collection
    // -------------------------------------------------------------------------

    private List<AttributeBinding> collectAttributes(
            Class<?> builderClass,
            List<Class<?>> builderHierarchy,
            List<TypeXMLConfiguration> typeConfigs) {
        List<AttributeBinding> result = new ArrayList<>();
        for (Class<?> builderLevel : builderHierarchy) {
            for (Method getter : getOrderedGetters(builderLevel)) {
                result.add(buildAttributeBinding(getter, builderClass, typeConfigs));
            }
        }
        return result;
    }

    /**
     * Returns getter methods declared directly on {@code builderLevel}, filtered to
     * those annotated with {@code @RosettaAttribute} and {@code @Accessor(GETTER)},
     * sorted by field declaration order.
     *
     * <p>{@link Class#getDeclaredMethods()} does not guarantee source order on Java 9+,
     * but {@link Class#getDeclaredFields()} does (the JVM spec preserves field order from
     * the class file, which javac writes in source order). We build a field-position map
     * and sort the getters by the position of their logical name in that map.
     */
    private List<Method> getOrderedGetters(Class<?> builderLevel) {
        Map<String, Integer> fieldOrder = new HashMap<>();
        int idx = 0;
        for (Field f : builderLevel.getDeclaredFields()) {
            if (!f.isSynthetic()) {
                fieldOrder.put(f.getName(), idx++);
            }
        }

        List<Method> getters = new ArrayList<>();
        for (Method m : builderLevel.getDeclaredMethods()) {
            if (isAttributeGetter(m)) {
                getters.add(m);
            }
        }

        Collections.sort(getters, new java.util.Comparator<Method>() {
            public int compare(Method a, Method b) {
                String nameA = a.getAnnotation(RosettaAttribute.class).value();
                String nameB = b.getAnnotation(RosettaAttribute.class).value();
                Integer posA = fieldOrder.get(nameA);
                Integer posB = fieldOrder.get(nameB);
                if (posA == null) posA = Integer.MAX_VALUE;
                if (posB == null) posB = Integer.MAX_VALUE;
                return posA.compareTo(posB);
            }
        });

        return getters;
    }

    @SuppressWarnings("deprecation")
    private boolean isAttributeGetter(Method m) {
        if (m.isBridge()) return false;
        if (!m.isAnnotationPresent(RosettaAttribute.class)) return false;
        Accessor accessor = m.getAnnotation(Accessor.class);
        if (accessor == null || accessor.value() != AccessorType.GETTER) return false;
        if (m.isAnnotationPresent(RosettaIgnore.class)) return false;
        if (m.isAnnotationPresent(RuneIgnore.class)) return false;
        return true;
    }

    private AttributeBinding buildAttributeBinding(
            Method getter, Class<?> builderClass, List<TypeXMLConfiguration> typeConfigs) {
        String logicalName = getter.getAnnotation(RosettaAttribute.class).value();
        boolean isMulti = getter.isAnnotationPresent(Multi.class);

        Class<?> rawElementType = getRawElementType(getter, isMulti);
        Class<?> valueType = unwrapBuilderType(rawElementType);
        boolean isRosettaModelObject = RosettaModelObject.class.isAssignableFrom(valueType);
        boolean isEnum = valueType.isEnum();

        Method setter = isMulti ? null : findSetter(builderClass, logicalName, valueType);
        Method adder = isMulti ? findAdder(builderClass, logicalName, valueType) : null;

        Optional<AttributeXMLConfiguration> attrConfig = getAttributeConfig(logicalName, typeConfigs);

        String xmlName = attrConfig.flatMap(AttributeXMLConfiguration::getXmlName).orElse(logicalName);
        AttributeXMLRepresentation xmlRepresentation = attrConfig
                .flatMap(AttributeXMLConfiguration::getXmlRepresentation)
                .orElse(AttributeXMLRepresentation.ELEMENT);

        @SuppressWarnings("deprecation")
        Optional<String> elementRef = attrConfig.flatMap(c ->
                c.getElementRef().isPresent() ? c.getElementRef() : c.getSubstitutionGroup());

        return new AttributeBinding(logicalName, getter, setter, adder, isMulti,
                valueType, isRosettaModelObject, isEnum, xmlName, xmlRepresentation, elementRef);
    }

    // -------------------------------------------------------------------------
    // Value-type resolution
    // -------------------------------------------------------------------------

    /**
     * Returns the raw (possibly builder) element type: for multi, unwraps the
     * {@code List<? extends X>} wildcard; for single, returns the return type directly.
     */
    private Class<?> getRawElementType(Method getter, boolean isMulti) {
        if (!isMulti) {
            return getter.getReturnType();
        }
        Type genericReturn = getter.getGenericReturnType();
        if (genericReturn instanceof ParameterizedType) {
            Type arg = ((ParameterizedType) genericReturn).getActualTypeArguments()[0];
            if (arg instanceof WildcardType) {
                Type[] upper = ((WildcardType) arg).getUpperBounds();
                if (upper.length > 0 && upper[0] instanceof Class) {
                    return (Class<?>) upper[0];
                }
            } else if (arg instanceof Class) {
                return (Class<?>) arg;
            }
        }
        return Object.class;
    }

    /**
     * If {@code type} is a {@link RosettaModelObjectBuilder} inner interface (e.g.,
     * {@code Foo.FooBuilder}), returns its declaring class ({@code Foo.class}).
     * Otherwise returns {@code type} unchanged.
     */
    private Class<?> unwrapBuilderType(Class<?> type) {
        if (RosettaModelObjectBuilder.class.isAssignableFrom(type)
                && type.getDeclaringClass() != null) {
            return type.getDeclaringClass();
        }
        return type;
    }

    // -------------------------------------------------------------------------
    // Setter / adder discovery
    // -------------------------------------------------------------------------

    private Method findSetter(Class<?> builderClass, String logicalName, Class<?> valueType) {
        String setterName = "set" + capitalize(logicalName);
        for (Method m : builderClass.getMethods()) {
            if (m.getName().equals(setterName)
                    && m.getParameterCount() == 1
                    && m.getParameterTypes()[0].isAssignableFrom(valueType)) {
                return m;
            }
        }
        return null;
    }

    private Method findAdder(Class<?> builderClass, String logicalName, Class<?> valueType) {
        String adderName = "add" + capitalize(logicalName);
        for (Method m : builderClass.getMethods()) {
            if (m.getName().equals(adderName)
                    && m.getParameterCount() == 1
                    && m.getParameterTypes()[0].isAssignableFrom(valueType)) {
                return m;
            }
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Attribute config lookup
    // -------------------------------------------------------------------------

    /**
     * Finds the {@link AttributeXMLConfiguration} for {@code logicalName} by walking
     * {@code typeConfigs} in most-derived-first order.
     */
    private Optional<AttributeXMLConfiguration> getAttributeConfig(
            String logicalName, List<TypeXMLConfiguration> typeConfigs) {
        for (TypeXMLConfiguration typeConfig : typeConfigs) {
            if (typeConfig.getAttributes().isPresent()) {
                Map<String, AttributeXMLConfiguration> attrs = typeConfig.getAttributes().get();
                if (attrs.containsKey(logicalName)) {
                    return Optional.of(attrs.get(logicalName));
                }
            }
        }
        return Optional.empty();
    }

    // -------------------------------------------------------------------------
    // Type-level XML metadata
    // -------------------------------------------------------------------------

    private String resolveXmlElementName(
            Optional<TypeXMLConfiguration> primaryConfig, String defaultName) {
        return primaryConfig.flatMap(TypeXMLConfiguration::getXmlElementName).orElse(defaultName);
    }

    private Optional<String> resolveXmlNamespace(Optional<TypeXMLConfiguration> primaryConfig) {
        return primaryConfig
                .flatMap(TypeXMLConfiguration::getXmlElementFullyQualifiedName)
                .map(this::extractNamespaceFromFqn);
    }

    /**
     * Extracts the namespace portion from a fully-qualified XML name of the form
     * {@code namespace/localName} — the convention the config's
     * {@code xmlElementFullyQualifiedName} field uses.
     * Returns {@code null} if there is no {@code /} in the string.
     */
    private String extractNamespaceFromFqn(String fqn) {
        int lastSlash = fqn.lastIndexOf('/');
        return lastSlash > 0 ? fqn.substring(0, lastSlash) : null;
    }

    /**
     * Merges constant XML attributes from all type configs.
     * More-derived configs are applied last so they override base configs.
     */
    private Map<String, String> resolveXmlConstantAttributes(List<TypeXMLConfiguration> typeConfigs) {
        Map<String, String> result = new LinkedHashMap<>();
        for (int i = typeConfigs.size() - 1; i >= 0; i--) {
            typeConfigs.get(i).getXmlAttributes().ifPresent(result::putAll);
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
