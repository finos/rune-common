package com.regnosys.rosetta.common.hashing;

/*-
 * ==============
 * Rune Common
 * ==============
 * Copyright (C) 2018 - 2024 REGnosys
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

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.regnosys.rosetta.common.translation.Path;
import com.regnosys.rosetta.common.util.PathUtils;
import com.regnosys.rosetta.common.util.SimpleBuilderProcessor;
import com.regnosys.rosetta.common.util.SimpleProcessor;
import com.rosetta.lib.postprocess.PostProcessorReport;
import com.rosetta.model.lib.GlobalKey;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.meta.FieldWithMeta;
import com.rosetta.model.lib.meta.GlobalKeyFields;
import com.rosetta.model.lib.meta.ReferenceWithMeta.ReferenceWithMetaBuilder;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.PostProcessStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

public class ReferenceResolverProcessStep implements PostProcessStep {

    private final ReferenceConfig referenceConfig;

    public ReferenceResolverProcessStep(ReferenceConfig referenceConfig) {
        this.referenceConfig = referenceConfig;
    }

    @Override
    public Integer getPriority() {
        return 2;
    }

    @Override
    public String getName() {
        return "Reference Resolver";
    }

    @Override
    public <T extends RosettaModelObject> ReferenceResolverPostProcessorReport runProcessStep(
            Class<? extends T> topClass,
            T instance) {
        RosettaPath path = RosettaPath.valueOf(instance.getType().getSimpleName());
        ReferenceCollector collector = new ReferenceCollector(referenceConfig);
        instance.process(path, collector);
        ReferenceResolver resolver =
                new ReferenceResolver(referenceConfig, collector.globalReferences, collector.helper);
        RosettaModelObjectBuilder builder = instance.toBuilder();
        builder.process(path, resolver);
        return new ReferenceResolverPostProcessorReport<T>((T) builder.build());
    }

    private static class ReferenceCollector extends SimpleProcessor {

        private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceCollector.class);

        // Table:
        // - Class<?>: referenced Class<?> (e.g. Quantity or QuantityBuilder)
        // - String: reference key value (e.g. "quantity-1")
        // - Object: referenced object (e.g. populated Quantity object to be set on ReferenceWithMetaQuantity.value)
        private final Table<Class<?>, String, Object> globalReferences = HashBasedTable.create();
        private final ScopeReferenceHelper<Table<Class<?>, String, Object>> helper;

        public ReferenceCollector(ReferenceConfig referenceConfig) {
            this.helper = new ScopeReferenceHelper<>(referenceConfig, () -> HashBasedTable.create());
        }

        @Override
        public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path,
                                                                     Class<? extends R> rosettaType,
                                                                     R instance,
                                                                     RosettaModelObject parent,
                                                                     AttributeMeta... metas) {
            helper.collectScopePath(path, rosettaType);

            if (instance instanceof GlobalKey && instance != null) {
                GlobalKey globalKey = (GlobalKey) instance;
                Object value = getValue(instance);
                Class<?> valueClass = getValueType(instance);
                if (value != null && valueClass != null) {
                    ofNullable(globalKey.getMeta())
                            .map(GlobalKeyFields::getGlobalKey)
                            .ifPresent(gk -> globalReferences.put(valueClass, gk, value));
                    of(globalKey)
                            .map(GlobalKey::getMeta)
                            .map(GlobalKeyFields::getKey)
                            .ifPresent(keys -> keys.stream()
                                    .filter(k -> k.getKeyValue() != null)
                                    .forEach(k -> {
                                        String keyValue = k.getKeyValue();
                                        Path keyPath = PathUtils.toPath(path);
                                        LOGGER.debug("Collecting object [key={}, type={}, path={}]",
                                                keyValue, valueClass.getName(), path);
                                        helper.getDataForModelPath(keyPath)
                                                .put(valueClass, keyValue, value);
                                    }));
                }
            }
            return true;
        }

        private Object getValue(RosettaModelObject instance) {
            if (instance instanceof FieldWithMeta) {
                return ((FieldWithMeta<?>) instance).getValue();
            } else
                return instance;
        }

        private Class<?> getValueType(RosettaModelObject builder) {
            if (builder instanceof FieldWithMeta) {
                return ((FieldWithMeta<?>) builder).getValueType();
            } else
                return builder.getType();
        }

        @Override
        public Report report() {
            return null;
        }
    }

    private static class ReferenceResolver extends SimpleBuilderProcessor {

        private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceResolver.class);

        private final Table<Class<?>, String, Object> globalReferences;
        private final ScopeReferenceHelper<Table<Class<?>, String, Object>> helper;
        private final ReferenceConfig config;

        private ReferenceResolver(
                ReferenceConfig config,
                Table<Class<?>, String, Object> globalReferences,
                ScopeReferenceHelper<Table<Class<?>, String, Object>> helper) {
            this.config = config;
            this.globalReferences = globalReferences;
            this.helper = helper;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path,
                                                                     Class<R> rosettaType,
                                                                     RosettaModelObjectBuilder builder,
                                                                     RosettaModelObjectBuilder parent,
                                                                     AttributeMeta... metas) {
            if (config.getExcludedPaths().stream().anyMatch(endsWithPathElement -> path.endsWith(endsWithPathElement))) {
                return false;
            }
            if (builder instanceof ReferenceWithMetaBuilder) {
                ReferenceWithMetaBuilder referenceWithMetaBuilder = (ReferenceWithMetaBuilder) builder;
                if (referenceWithMetaBuilder.getGlobalReference() != null) {
                    setReferenceValue(referenceWithMetaBuilder.getGlobalReference(),
                            globalReferences,
                            referenceWithMetaBuilder,
                            path);
                } else if (referenceWithMetaBuilder.getReference() != null) {
                    Path currentModelPath = PathUtils.toPath(path);
                    Table<Class<?>, String, Object> currentScopeReferences = helper.getDataForModelPath(currentModelPath);
                    if (currentScopeReferences != null) {
                        setReferenceValue(referenceWithMetaBuilder.getReference().getReference(),
                                currentScopeReferences,
                                referenceWithMetaBuilder,
                                path);
                    }
                }
            }
            return true;
        }

        private void setReferenceValue(String keyValue, Table<Class<?>, String, Object> references, ReferenceWithMetaBuilder referenceWithMeta, RosettaPath path) {
            if (keyValue != null) {
                Map<Class<?>, Object> clazzToReferencedObjectMap = references.column(keyValue);
                if (clazzToReferencedObjectMap != null) {
                    List<Entry<Class<?>, Object>> clazzToReferencedObjectEntries = clazzToReferencedObjectMap.entrySet().stream()
                            .filter(e -> doTest(referenceWithMeta.getValueType(), e.getKey()))
                            .collect(Collectors.toList());
                    clazzToReferencedObjectEntries.stream()
                            .map(Entry::getValue)
                            .forEach(referencedObject -> {
                                LOGGER.debug("Setting resolved object [key={}, type={}, path={}, scope={}]",
                                        keyValue, referenceWithMeta.getValueType().getName(), path,
                                        Optional.ofNullable(config).map(ReferenceConfig::getScopeType).orElse(null));
                                referenceWithMeta.setValue(referencedObject);
                            });
                }
            }
        }

        private boolean doTest(Class<?> valueType, Class<?> clazz) {
            return valueType.isAssignableFrom(clazz);
        }

        @Override
        public Report report() {
            return null;
        }
    }

    public static class ReferenceResolverPostProcessorReport<T extends RosettaModelObject> implements PostProcessorReport {
        private final T instance;

        private ReferenceResolverPostProcessorReport(T instance) {
            this.instance = instance;
        }

        @Override
        public T getResultObject() {
            return this.instance;
        }
    }
}
