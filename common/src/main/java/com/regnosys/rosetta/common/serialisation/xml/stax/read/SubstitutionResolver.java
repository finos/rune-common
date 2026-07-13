package com.regnosys.rosetta.common.serialisation.xml.stax.read;

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

import com.regnosys.rosetta.common.serialisation.xml.config.RosettaXMLConfiguration;
import com.regnosys.rosetta.common.serialisation.xml.config.TypeXMLConfiguration;
import com.rosetta.model.lib.ModelSymbolId;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.util.DottedPath;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Resolves the concrete Java type substituted for an {@code elementRef} XML substitution
 * group, given the XML element name and namespace actually encountered on read.
 *
 * <p>This is the read-side (reverse) counterpart to the Jackson-era
 * {@code RosettaXMLAnnotationIntrospector#findSubstitutionMap}: instead of building a Jackson
 * {@code SubstitutionMap} keyed by
 * {@code JavaType}, it builds the equivalent index directly from {@link RosettaXMLConfiguration}
 * and resolves plain {@code Class<?>} candidates, so the StAX reader needs no Jackson type.
 *
 * <h3>Resolution order (mirrors the Jackson-era {@code SubstitutedMethodProperty})</h3>
 * <ol>
 *   <li>Exact match on (namespace URI, local name) — the namespace-aware path that fixes
 *       issue 6 (same local name across namespaces, e.g. FiML vs FpML {@code commodityOption}).</li>
 *   <li>Local-name-only fallback — used when the config carries no namespace for a candidate
 *       (legacy V1/V2 configs), or when only one candidate exists regardless of namespace.</li>
 * </ol>
 *
 * <h3>Group membership</h3>
 * A concrete type belongs to the group for {@code elementRef} when either:
 * <ul>
 *   <li>its own {@code xmlElementFullyQualifiedName} equals {@code elementRef} (the group head
 *       element itself is concrete), or</li>
 *   <li>its {@code substitutionGroup} config field equals {@code elementRef}, transitively —
 *       a member's own element name becomes the next group key, so substitution chains
 *       (e.g. {@code fish} substitutes {@code animal}; {@code salmon} substitutes {@code fish})
 *       resolve correctly. Abstract types (e.g. {@code fish}) are excluded from the final
 *       candidate list but still participate in the transitive walk.</li>
 * </ul>
 * The legacy V1 {@code substitutionFor} field (a direct {@code ModelSymbolId} back-reference to
 * the head type, superseded by {@code substitutionGroup}) is also honoured, keyed off the
 * attribute's statically declared head type.
 */
public class SubstitutionResolver {

    private final RosettaXMLConfiguration config;
    private final ClassLoader classLoader;

    private Map<String, ModelSymbolId> elementIndex;
    private Map<String, List<ModelSymbolId>> substitutionGroupIndex;
    private final Map<String, SubstitutionGroup> resolvedGroups = new HashMap<String, SubstitutionGroup>();

    public SubstitutionResolver(RosettaXMLConfiguration config, ClassLoader classLoader) {
        this.config = config;
        this.classLoader = classLoader;
    }

    /**
     * Resolves the concrete type substituted for {@code elementRef} that matches the given
     * XML element name/namespace, or {@code null} when no substitution candidate matches.
     *
     * @param elementRef  the substitution-group head name from the attribute's config
     *                    (e.g. {@code "urn:my.schema/animal"})
     * @param headType    the attribute's statically declared value type — used only for the
     *                    legacy V1 {@code substitutionFor} fallback
     * @param localName   the actual child element's local name
     * @param namespaceURI the actual child element's namespace URI, or {@code null}/empty if none
     */
    public Class<?> resolve(String elementRef, Class<?> headType, String localName, String namespaceURI) {
        SubstitutionGroup group = getOrBuildGroup(elementRef, headType);

        String ns = (namespaceURI == null) ? XMLConstants.NULL_NS_URI : namespaceURI;
        Class<?> exact = group.byQualifiedName.get(new QName(ns, localName));
        if (exact != null) {
            return exact;
        }

        List<Class<?>> candidates = group.byLocalName.get(localName);
        if (candidates != null && !candidates.isEmpty()) {
            return candidates.get(0);
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Group resolution + caching
    // -------------------------------------------------------------------------

    private static final class SubstitutionGroup {
        final Map<QName, Class<?>> byQualifiedName = new LinkedHashMap<QName, Class<?>>();
        final Map<String, List<Class<?>>> byLocalName = new LinkedHashMap<String, List<Class<?>>>();

        void add(String localName, String namespace, Class<?> type) {
            String ns = (namespace == null) ? XMLConstants.NULL_NS_URI : namespace;
            byQualifiedName.put(new QName(ns, localName), type);
            List<Class<?>> list = byLocalName.get(localName);
            if (list == null) {
                list = new ArrayList<Class<?>>();
                byLocalName.put(localName, list);
            }
            list.add(type);
        }
    }

    private SubstitutionGroup getOrBuildGroup(String elementRef, Class<?> headType) {
        SubstitutionGroup cached = resolvedGroups.get(elementRef);
        if (cached != null) {
            return cached;
        }
        ensureIndexesBuilt();

        SubstitutionGroup group = new SubstitutionGroup();
        populateFromElementIndex(elementRef, group);
        populateFromSubstitutionGroupIndex(elementRef, group, new HashSet<String>());
        populateFromLegacySubstitutionFor(headType, group);

        resolvedGroups.put(elementRef, group);
        return group;
    }

    private void ensureIndexesBuilt() {
        if (elementIndex != null) {
            return;
        }
        elementIndex = new HashMap<String, ModelSymbolId>();
        substitutionGroupIndex = new HashMap<String, List<ModelSymbolId>>();

        for (Map.Entry<ModelSymbolId, TypeXMLConfiguration> entry : config.getTypeConfigMap().entrySet()) {
            ModelSymbolId symbolId = entry.getKey();
            TypeXMLConfiguration cfg = entry.getValue();

            if (cfg.getXmlElementFullyQualifiedName().isPresent()) {
                elementIndex.put(cfg.getXmlElementFullyQualifiedName().get(), symbolId);
            }
            if (cfg.getSubstitutionGroup().isPresent()) {
                String group = cfg.getSubstitutionGroup().get();
                List<ModelSymbolId> members = substitutionGroupIndex.get(group);
                if (members == null) {
                    members = new ArrayList<ModelSymbolId>();
                    substitutionGroupIndex.put(group, members);
                }
                members.add(symbolId);
            }
        }
    }

    /**
     * The group head element itself may be concrete (its own {@code xmlElementFullyQualifiedName}
     * equals {@code elementRef}) — add it as a candidate.
     */
    private void populateFromElementIndex(String elementRef, SubstitutionGroup group) {
        ModelSymbolId id = elementIndex.get(elementRef);
        if (id == null) {
            return;
        }
        Optional<TypeXMLConfiguration> cfg = config.getConfigurationForType(id);
        if (cfg.isPresent() && !cfg.get().getAbstract().orElse(false)) {
            addCandidate(id, cfg.get(), group);
        }
    }

    /**
     * Transitively walks {@code substitutionGroup} members, recursing through intermediate
     * (possibly abstract) group heads via their own {@code xmlElementFullyQualifiedName}.
     */
    private void populateFromSubstitutionGroupIndex(
            String groupKey, SubstitutionGroup group, Set<String> visited) {
        if (!visited.add(groupKey)) {
            return;
        }
        List<ModelSymbolId> members = substitutionGroupIndex.get(groupKey);
        if (members == null) {
            return;
        }
        for (ModelSymbolId id : members) {
            Optional<TypeXMLConfiguration> cfgOpt = config.getConfigurationForType(id);
            if (!cfgOpt.isPresent()) {
                continue;
            }
            TypeXMLConfiguration cfg = cfgOpt.get();
            if (!cfg.getAbstract().orElse(false)) {
                addCandidate(id, cfg, group);
            }
            if (cfg.getXmlElementFullyQualifiedName().isPresent()) {
                populateFromSubstitutionGroupIndex(cfg.getXmlElementFullyQualifiedName().get(), group, visited);
            }
        }
    }

    /**
     * Legacy V1 fallback: {@code substitutionFor} is a deprecated {@code ModelSymbolId}
     * field on the substitute type pointing directly at the head type, superseded by
     * {@code substitutionGroup}. Only consulted when {@code headType} is known.
     */
    private void populateFromLegacySubstitutionFor(Class<?> headType, SubstitutionGroup group) {
        if (headType == null) {
            return;
        }
        ModelSymbolId headId = toModelSymbolId(headType);
        List<ModelSymbolId> substitutions = config.getSubstitutionsForType(headId);
        if (substitutions.isEmpty()) {
            return;
        }
        for (ModelSymbolId id : substitutions) {
            Optional<TypeXMLConfiguration> cfgOpt = config.getConfigurationForType(id);
            String elementName = cfgOpt.flatMap(TypeXMLConfiguration::getXmlElementName).orElse(id.getName());
            try {
                Class<?> type = classLoader.loadClass(id.getQualifiedName().toString());
                // Legacy substitutions carry no namespace information.
                group.add(elementName, null, type);
            } catch (ClassNotFoundException e) {
                // type not on classpath — skip
            }
        }
    }

    private void addCandidate(ModelSymbolId id, TypeXMLConfiguration cfg, SubstitutionGroup group) {
        String localName = cfg.getXmlElementName().orElse(null);
        String namespace = null;
        if (cfg.getXmlElementFullyQualifiedName().isPresent()) {
            String fqn = cfg.getXmlElementFullyQualifiedName().get();
            int lastSlash = fqn.lastIndexOf('/');
            if (lastSlash > 0) {
                namespace = fqn.substring(0, lastSlash);
                if (localName == null) {
                    localName = fqn.substring(lastSlash + 1);
                }
            } else if (localName == null) {
                localName = fqn;
            }
        }
        if (localName == null) {
            return;
        }
        try {
            Class<?> type = classLoader.loadClass(id.getQualifiedName().toString());
            group.add(localName, namespace, type);
        } catch (ClassNotFoundException e) {
            // type not on classpath — skip
        }
    }

    private ModelSymbolId toModelSymbolId(Class<?> type) {
        String logicalName = null;
        RuneDataType runeAnn = type.getAnnotation(RuneDataType.class);
        if (runeAnn != null && !runeAnn.value().isEmpty()) {
            logicalName = runeAnn.value();
        } else {
            RosettaDataType rosettaAnn = type.getAnnotation(RosettaDataType.class);
            if (rosettaAnn != null && !rosettaAnn.value().isEmpty()) {
                logicalName = rosettaAnn.value();
            } else {
                logicalName = type.getSimpleName();
            }
        }
        String namespace = type.getPackage().getName();
        return new ModelSymbolId(DottedPath.splitOnDots(namespace), logicalName);
    }
}
