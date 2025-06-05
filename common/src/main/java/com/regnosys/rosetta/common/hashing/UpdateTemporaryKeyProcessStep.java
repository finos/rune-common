package com.regnosys.rosetta.common.hashing;

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

import com.regnosys.rosetta.common.util.PathUtils;
import com.regnosys.rosetta.common.util.SimpleProcessor;
import com.rosetta.lib.postprocess.PostProcessorReport;
import com.rosetta.model.lib.GlobalKey;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.meta.FieldWithMeta;
import com.rosetta.model.lib.meta.FieldWithMeta.FieldWithMetaBuilder;
import com.rosetta.model.lib.meta.GlobalKeyFields;
import com.rosetta.model.lib.meta.Key;
import com.rosetta.model.lib.meta.Reference;
import com.rosetta.model.lib.meta.ReferenceWithMeta.ReferenceWithMetaBuilder;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.PostProcessStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A post-processing step that re-keys scoped references in a Rosetta model.
 * This processor identifies references with temporary keys (containing
 * "[a-zA-Z]*-\$[0-9]*", e.g., price-$123456) and assigns them new sequential
 * keys based on their prefix (e.g., price-1).
 */
public class UpdateTemporaryKeyProcessStep implements PostProcessStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateTemporaryKeyProcessStep.class);

    private final ReferenceConfig referenceConfig;

    /**
     * Creates a new ScopedReferenceReKeyProcessStep with the specified reference configuration.
     *
     * @param referenceConfig the configuration for reference processing
     */
    public UpdateTemporaryKeyProcessStep(ReferenceConfig referenceConfig) {
        this.referenceConfig = referenceConfig;
    }

    @Override
    public Integer getPriority() {
        return 2;
    }

    @Override
    public String getName() {
        return "Update Temporary Key Processor";
    }

    @Override
    public <T extends RosettaModelObject> PostProcessorReport runProcessStep(
            Class<? extends T> topClass,
            T instance) {
        RosettaModelObjectBuilder builder = instance.toBuilder();
        ScopedKeyReferenceCollector collectedData = collectScopedKeyReferences(builder);
        updateReferences(collectedData.helper.getScopeToDataMap().values());
        return new ScopedReferenceReKeyPostProcessorReport<>(instance.build());
    }

    /**
     * Updates references with new sequential keys based on their prefix.
     *
     * @param values the collection of scoped data to update
     */
    private void updateReferences(Collection<ScopedData> values) {
        for (ScopedData scopedData : values) {
            Map<String, AtomicInteger> prefixIndexMap = new HashMap<>();
            // Use keys in the order they were collected
            for (String keyValue : scopedData.keyAndReferences.keySet()) {
                KeyAndReferences keyAndReferences = scopedData.keyAndReferences.get(keyValue);
                LOGGER.debug("Updating scoped key/references for {}", keyValue);
                String prefix = keyValue.split("-")[0];
                int index = prefixIndexMap.computeIfAbsent(prefix, k -> new AtomicInteger(1)).getAndIncrement();
                String newKeyValue = String.format("%s-%d", prefix, index);
                for (GlobalKey.GlobalKeyBuilder key : keyAndReferences.keys) {
                    key.getOrCreateMeta().setKey(Collections.singletonList(Key.builder()
                            .setKeyValue(newKeyValue)
                            .setScope("DOCUMENT")));
                }
                for (ReferenceWithMetaBuilder<?> reference : keyAndReferences.references) {
                    reference.setReference(Reference.builder()
                            .setReference(newKeyValue)
                            .setScope("DOCUMENT"));
                }
            }
        }
    }

    /**
     * Collects scoped key references from the given instance.
     *
     * @param builder the instance to collect references from
     * @return the collector containing the collected references
     */
    private ScopedKeyReferenceCollector collectScopedKeyReferences(RosettaModelObjectBuilder builder) {
        RosettaPath path = RosettaPath.valueOf(builder.getType().getSimpleName());
        ScopedKeyReferenceCollector collector = new ScopedKeyReferenceCollector(referenceConfig);
        builder.process(path, collector);
        return collector;
    }

    /**
     * PostProcessorReport implementation for ScopedReferenceReKeyProcessStep.
     *
     * @param <T> the type of the Rosetta model object
     */
    public static class ScopedReferenceReKeyPostProcessorReport<T extends RosettaModelObject> implements PostProcessorReport {
        private final T instance;

        private ScopedReferenceReKeyPostProcessorReport(T instance) {
            this.instance = instance;
        }

        @Override
        public T getResultObject() {
            return this.instance;
        }
    }

    /**
     * Container for scoped data, holding key and reference information.
     */
    private static class ScopedData {
        private final Map<String, KeyAndReferences> keyAndReferences = new LinkedHashMap<>();

        /**
         * Adds a key to the scoped data.
         *
         * @param keyValue the key value
         * @param key      the global key builder
         */
        void addKey(String keyValue, GlobalKey.GlobalKeyBuilder key) {
            keyAndReferences.compute(keyValue, (k, oldValue) -> {
                KeyAndReferences newValue = oldValue == null ? new KeyAndReferences() : oldValue;
                newValue.keys.add(key);
                return newValue;
            });
        }

        /**
         * Adds a reference to the scoped data.
         *
         * @param keyValue  the key value
         * @param reference the reference builder
         */
        void addReference(String keyValue, ReferenceWithMetaBuilder<?> reference) {
            keyAndReferences.compute(keyValue, (k, oldValue) -> {
                KeyAndReferences newValue = oldValue == null ? new KeyAndReferences() : oldValue;
                newValue.references.add(reference);
                return newValue;
            });
        }
    }

    /**
     * Container for keys and references associated with a specific key value.
     */
    private static class KeyAndReferences {
        private final List<GlobalKey.GlobalKeyBuilder> keys = new ArrayList<>();
        private final List<ReferenceWithMetaBuilder<?>> references = new ArrayList<>();
    }


    /**
     * Processor that collects scoped key references from a Rosetta model.
     */
    private static class ScopedKeyReferenceCollector extends SimpleProcessor {

        private static final Logger LOGGER = LoggerFactory.getLogger(ScopedKeyReferenceCollector.class);
        // Regex to identify temporary keys
        private static final String TEMPORARY_KEY_REGEX = "[a-zA-Z]*-\\$[0-9]*";

        private final ScopeReferenceHelper<ScopedData> helper;

        /**
         * Creates a new ScopedKeyReferenceCollector with the specified reference configuration.
         *
         * @param referenceConfig the configuration for reference processing
         */
        public ScopedKeyReferenceCollector(ReferenceConfig referenceConfig) {
            this.helper = new ScopeReferenceHelper<>(referenceConfig, ScopedData::new);
        }

        @Override
        public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path,
                                                                     Class<? extends R> rosettaType,
                                                                     R instance,
                                                                     RosettaModelObject parent,
                                                                     AttributeMeta... metas) {
            helper.collectScopePath(path, rosettaType);

            if (instance instanceof FieldWithMeta<?>) {
                FieldWithMeta<?> fieldWithMeta = (FieldWithMeta<?>) instance;
                if (fieldWithMeta instanceof ReferenceWithMetaBuilder<?>) {
                    ReferenceWithMetaBuilder<?> referenceWithMeta = (ReferenceWithMetaBuilder<?>) fieldWithMeta;
                    Optional.ofNullable(referenceWithMeta.getReference()).map(Reference::getReference)
                            .filter(v -> v.matches(TEMPORARY_KEY_REGEX))
                            .ifPresent(referenceValue -> {
                                ScopedData scopedData = helper.getDataForModelPath(PathUtils.toPath(path));
                                LOGGER.debug("Collecting reference {} for type {} at path {}", referenceValue, referenceWithMeta.getValueType().getName(), path);
                                scopedData.addReference(referenceValue, referenceWithMeta);
                            });
                } else if (fieldWithMeta instanceof GlobalKey.GlobalKeyBuilder) {
                    GlobalKey.GlobalKeyBuilder globalKeyBuilder = (GlobalKey.GlobalKeyBuilder) fieldWithMeta;
                    Optional.ofNullable(globalKeyBuilder.getMeta()).map(GlobalKeyFields.GlobalKeyFieldsBuilder::getKey).orElse(Collections.emptyList()).stream().map(Key::getKeyValue).filter(Objects::nonNull).findFirst()
                            .filter(v -> v.matches(TEMPORARY_KEY_REGEX))
                            .ifPresent(keyValue -> {
                                ScopedData scopedData = helper.getDataForModelPath(PathUtils.toPath(path));
                                LOGGER.debug("Collecting key {} for type {} at path {}", keyValue, fieldWithMeta.getValueType().getName(), path);
                                scopedData.addKey(keyValue, globalKeyBuilder);
                            });
                }
            }
            return true;
        }

        @Override
        public Report report() {
            // No report needed for this processor
            return null;
        }
    }
}
