package com.regnosys.rosetta.common.hashing;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.rosetta.lib.postprocess.PostProcessorReport;
import com.rosetta.model.lib.GlobalKeyBuilder;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.meta.GlobalKeyFields;
import com.rosetta.model.lib.meta.ReferenceWithMetaBuilder;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.PostProcessStep;

import java.util.Optional;

import static java.util.Optional.ofNullable;

public class ReferenceResolverProcessStep implements PostProcessStep {

    @Override
    public Integer getPriority() {
        return 3;
    }

    @Override
    public String getName() {
        return "Reference Resolver";
    }

    @Override
    public <T extends RosettaModelObject> ReferenceResolverPostProcessorReport runProcessStep(Class<T> topClass, RosettaModelObjectBuilder builder) {
        RosettaPath path = RosettaPath.valueOf(topClass.getSimpleName());
        ReferenceCollector referenceCollector = new ReferenceCollector();
        builder.process(path, referenceCollector);
        ReferenceResolver referenceResolver = new ReferenceResolver(referenceCollector.references);
        builder.process(path, referenceResolver);
        referenceResolver.report();
        return new ReferenceResolverPostProcessorReport(referenceCollector.references, builder);
    }

    private static class ReferenceCollector extends SimpleBuilderProcessor {

        private final Table<Class<?>, String, Object> references = HashBasedTable.create();

        @Override
        public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path, Class<R> rosettaType, RosettaModelObjectBuilder builder, RosettaModelObjectBuilder parent, AttributeMeta... metas) {
            if (builder instanceof GlobalKeyBuilder) {
                GlobalKeyBuilder globalKeyBuilder = (GlobalKeyBuilder) builder;
                ofNullable(globalKeyBuilder.getMeta()).map(GlobalKeyFields::getGlobalKey)
                        .ifPresent(globalKey -> references.put(rosettaType, globalKey, builder));
            }
            return true;
        }

        @Override
        public <T> void processBasic(RosettaPath path, Class<T> rosettaType, T instance, RosettaModelObjectBuilder parent, AttributeMeta... metas) {
            // Basic type reference collecting is not supported yet.
        }

        @Override
        public Report report() {
            return new ReferenceResolverReport(references);
        }
    }

    private static class ReferenceResolver extends SimpleBuilderProcessor {

        private final Table<Class<?>, String, Object> references;

        private ReferenceResolver(Table<Class<?>, String, Object> refs) {
            this.references = refs;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path, Class<R> rosettaType, RosettaModelObjectBuilder builder, RosettaModelObjectBuilder parent, AttributeMeta... metas) {
            if (builder instanceof ReferenceWithMetaBuilder) {
                ReferenceWithMetaBuilder referenceWithMetaBuilder = (ReferenceWithMetaBuilder) builder;
                if (referenceWithMetaBuilder.getValue() == null && referenceWithMetaBuilder.getGlobalReference() != null) {
                    ofNullable(references.get(referenceWithMetaBuilder.getValueType(), referenceWithMetaBuilder.getGlobalReference()))
                            .map(RosettaModelObjectBuilder.class::cast)
                            .ifPresent(b -> referenceWithMetaBuilder.setValue(b.build()));
                }
            }
            return true;
        }

        @Override
        public <T> void processBasic(RosettaPath path, Class<T> rosettaType, T instance, RosettaModelObjectBuilder parent, AttributeMeta... metas) {
            // Basic type reference resolving is not supported yet.
        }

        @Override
        public Report report() {
            return new ReferenceResolverReport(references);
        }
    }

    static class ReferenceResolverReport implements BuilderProcessor.Report {
        private final Table<Class<?>, String, Object> references;

        private ReferenceResolverReport(Table<Class<?>, String, Object> refs) {
            this.references = refs;
        }

        public Table<Class<?>, String, Object> getReferences() {
            return references;
        }
    }

    public static class ReferenceResolverPostProcessorReport implements PostProcessorReport {
        private final Table<Class<?>, String, Object> references;
        private final RosettaModelObjectBuilder builder;

        private ReferenceResolverPostProcessorReport(Table<Class<?>, String, Object> refs, RosettaModelObjectBuilder builder) {
            this.references = refs;
            this.builder = builder;
        }

        public Table<Class<?>, String, Object> getReferences() {
            return this.references;
        }

        public <T extends RosettaModelObject> Optional<T> getReferencedObject(Class<T> rosettaType, String globalReference) {
            return Optional.ofNullable(references.get(rosettaType, globalReference))
                    .map(RosettaModelObjectBuilder.class::cast)
                    .map(RosettaModelObjectBuilder::build)
                    .map(o -> (T) o);
        }

        @Override
        public RosettaModelObjectBuilder getResultObject() {
            return this.builder;
        }
    }


}
