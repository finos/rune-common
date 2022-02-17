package com.regnosys.rosetta.common.hashing;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
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
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.PostProcessStep;
import com.rosetta.model.lib.process.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

public class ReferenceResolverProcessStep implements PostProcessStep {

    private final ReferenceResolverConfig config;

    public ReferenceResolverProcessStep(ReferenceResolverConfig config) {
        this.config = config;
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
        RosettaPath path = RosettaPath.valueOf(topClass.getSimpleName());
        ReferenceCollector collector = new ReferenceCollector(config.getScopeType());
        instance.process(path, collector);
        ReferenceResolver resolver =
                new ReferenceResolver(config, collector.globalReferences, collector.scopeReferences);
        RosettaModelObjectBuilder builder = instance.toBuilder();
        builder.process(path, resolver);
        resolver.report();
        return new ReferenceResolverPostProcessorReport(collector.globalReferences, collector.scopeReferences, builder);
    }

    private static class ReferenceCollector extends SimpleProcessor {

        private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceCollector.class);

        // Table:
        // - Class<?>: referenced Class<?> (e.g. Quantity or QuantityBuilder)
        // - String: reference key value (e.g. "quantity-1")
        // - Object: referenced object (e.g. populated Quantity object to be set on ReferenceWithMetaQuantity.value)
        private final Table<Class<?>, String, Object> globalReferences = HashBasedTable.create();
        private final Map<RosettaPath, Table<Class<?>, String, Object>> scopeReferences = new ConcurrentHashMap<>();
        private final AtomicReference<RosettaPath> currentScopePath = new AtomicReference<>(RosettaPath.valueOf("emptyScope"));
        private final Class<?> scopeType;

        public ReferenceCollector(Class<?> scopeType) {
            this.scopeType = scopeType;
        }

        @Override
        public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path,
                                                                     Class<? extends R> rosettaType,
                                                                     R instance,
                                                                     RosettaModelObject parent,
                                                                     AttributeMeta... metas) {
            if (scopeType != null && scopeType.isAssignableFrom(rosettaType)) {
                currentScopePath.set(path);
            }
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
                                        LOGGER.debug("Collecting object [key={}, type={}, path={}, scope={}]",
                                                keyValue, valueClass.getName(), path, currentScopePath.get());
                                        scopeReferences
                                                .computeIfAbsent(currentScopePath.get(), key -> HashBasedTable.create())
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
            return new ReferenceResolverReport(globalReferences, scopeReferences);
        }
    }

    private static class ReferenceResolver extends SimpleBuilderProcessor {

        private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceResolver.class);

        private final Table<Class<?>, String, Object> globalReferences;
        private final Map<RosettaPath, Table<Class<?>, String, Object>> scopeReferences;
        private final AtomicReference<RosettaPath> currentScopePath = new AtomicReference<>(RosettaPath.valueOf("emptyScope"));
        private final ReferenceResolverConfig config;

        private ReferenceResolver(
                ReferenceResolverConfig config,
                Table<Class<?>, String, Object> globalReferences,
                Map<RosettaPath, Table<Class<?>, String, Object>> scopeReferences) {
            this.config = config;
            this.globalReferences = globalReferences;
            this.scopeReferences = scopeReferences;
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
            if (config.getScopeType() != null && config.getScopeType().isAssignableFrom(rosettaType)) {
                currentScopePath.set(path);
            }
            if (builder instanceof ReferenceWithMetaBuilder) {
                ReferenceWithMetaBuilder referenceWithMetaBuilder = (ReferenceWithMetaBuilder) builder;
                if (referenceWithMetaBuilder.getGlobalReference() != null) {
                    setReferenceValue(referenceWithMetaBuilder.getGlobalReference(),
                            globalReferences,
                            referenceWithMetaBuilder,
                            path);
                } else if (referenceWithMetaBuilder.getReference() != null) {
                    Table<Class<?>, String, Object> currentScopeReferences = scopeReferences.get(currentScopePath.get());
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
                                        keyValue, referenceWithMeta.getValueType().getName(), path, currentScopePath.get());
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
            return new ReferenceResolverReport(globalReferences, scopeReferences);
        }
    }

    static class ReferenceResolverReport implements BuilderProcessor.Report, Processor.Report {
        private final Table<Class<?>, String, Object> globalReferences;
        private final Map<RosettaPath, Table<Class<?>, String, Object>> scopeReferences;

        private ReferenceResolverReport(
                Table<Class<?>, String, Object> globalReferences,
                Map<RosettaPath, Table<Class<?>, String, Object>> scopeReferences) {
            this.globalReferences = globalReferences;
            this.scopeReferences = scopeReferences;
        }

        public Table<Class<?>, String, Object> getGlobalReferences() {
            return globalReferences;
        }

        public Map<RosettaPath, Table<Class<?>, String, Object>> getScopeReferences() {
            return scopeReferences;
        }
    }

    public static class ReferenceResolverPostProcessorReport implements PostProcessorReport {
        private final Table<Class<?>, String, Object> globalReferences;
        private final Map<RosettaPath, Table<Class<?>, String, Object>> scopeReferences;
        private final RosettaModelObjectBuilder builder;

        private ReferenceResolverPostProcessorReport(Table<Class<?>, String, Object> globalReferences,
                                                     Map<RosettaPath, Table<Class<?>, String, Object>> scopeReferences,
                                                     RosettaModelObjectBuilder builder) {
            this.globalReferences = globalReferences;
            this.scopeReferences = scopeReferences;
            this.builder = builder;
        }

        public Table<Class<?>, String, Object> getGlobalReferences() {
            return this.globalReferences;
        }

        public Map<RosettaPath, Table<Class<?>, String, Object>> getScopeReferences() {
            return scopeReferences;
        }

        @SuppressWarnings("unchecked")
        public <T extends RosettaModelObject> Optional<T> getReferencedObject(Class<T> rosettaType,
                                                                              String globalReference) {
            return Optional.ofNullable(globalReferences.get(rosettaType, globalReference))
                    .map(RosettaModelObjectBuilder.class::cast).map(RosettaModelObjectBuilder::build).map(o -> (T) o);
        }

        @Override
        public RosettaModelObjectBuilder getResultObject() {
            return this.builder;
        }
    }
}
