package com.regnosys.rosetta.common.hashing;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.meta.BasicReferenceWithMetaBuilder;
import com.rosetta.model.lib.meta.GlobalKeyFields;
import com.rosetta.model.lib.meta.ReferenceWithMetaBuilder;
import com.rosetta.model.lib.meta.TemplateFields;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.Processor;

import java.util.Arrays;
import java.util.Optional;

/**
 * A simple implementation of {@link Processor and BuilderProcessor} that only considers non-null
 * values.
 * For all non-null primitive values it uses the accumulate method of the integer report to accumulate a hashcode
 */
public class NonNullHashCollector extends SimpleBuilderProcessor implements Processor {

    private final IntegerHashGenerator hashcodeGenerator;
    protected final IntegerReport report;

    public NonNullHashCollector() {
        this.hashcodeGenerator = new IntegerHashGenerator();
        report = new IntegerReport(0);
    }

    @Override
    public <R extends RosettaModelObject> void processRosetta(RosettaPath path, Class<R> rosettaType,
                                                              R instance, RosettaModelObject parent, AttributeMeta... metas) {
        if (instance != null && !metaContains(metas, AttributeMeta.META)) {
            report.accumulate();
        }
    }

    @Override
    public <T> void processBasic(RosettaPath path, Class<T> rosettaType, T instance, RosettaModelObject parent,
                                 AttributeMeta... metas) {
        if (instance != null && !metaContains(metas, AttributeMeta.META)) {
            int hash = hashcodeGenerator.generate(instance);
            report.accumulate(hash);
        }

    }

    @Override
    public IntegerReport report() {
        return report;
    }

    @Override
    public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path, Class<R> rosettaType,
                                                                 RosettaModelObjectBuilder builder, RosettaModelObjectBuilder parent, AttributeMeta... metas) {
        Result result = shouldIncludeInHash(builder, parent, metas);
        if (result.includeInHash) {
            report.accumulate();
        }
        return result.continueProcessing;
    }

    @Override
    public <T> void processBasic(RosettaPath path, Class<T> rosettaType, T instance,
                                 RosettaModelObjectBuilder parent, AttributeMeta... metas) {
        if (instance != null && !metaContains(metas, AttributeMeta.META)) {
            int hash = hashcodeGenerator.generate(instance);
            report.accumulate(hash);
        }
    }

    /**
     * Should include in hash if:
     * -  metas is empty and we don't have a MetaFieldsBuilder - Its a regular attribute that we need to hash
     * -  Have a MetaFieldsBuilder and an IS_META meta attribute - This is meta attribute we want to hash like scheme
     */
    private Result shouldIncludeInHash(RosettaModelObjectBuilder builder, RosettaModelObjectBuilder parent, AttributeMeta[] metas) {
        if (builder == null || !builder.hasData()) {
            return new Result(false, false);
        }
        if (isGlobalKeyFieldsBuilder(builder)) {
            // do not include meta folder in hash, however it's contents maybe included
            return new Result(false, true);
        }
        if (isTemplateFieldsBuilder(builder)) {
            // do not include template folder in hash
            return new Result(false, false);
        }
        if (isReferenceWithMetaBuilder(builder) || isBasicReferenceWithMetaBuilder(builder)) {
            // do not include reference folder in hash, however it's contents maybe included (e.g if it's a value with no reference)
            return new Result(false, true);
        }
        if (isReferenceWithMetaContainingReference(parent)) {
            // if the parent contains a reference, don't include any children in the hash (and stop processing)
            return new Result(false, false);
        }
        if (isBasicReferenceWithMetaContainingReference(parent)) {
            // if the parent contains a reference, don't include any children in the hash (and stop processing)
            return new Result(false, false);
        }
        if (metaContains(metas, AttributeMeta.GLOBAL_KEY)
                || metaContains(metas, AttributeMeta.EXTERNAL_KEY)
                || metaContains(metas, AttributeMeta.GLOBAL_KEY_FIELD)) {
            return new Result(false, false);
        }
        return new Result(true, true);
    }

    private boolean isReferenceWithMetaContainingReference(RosettaModelObjectBuilder builder) {
        if (isReferenceWithMetaBuilder(builder)) {
            ReferenceWithMetaBuilder<?> refBuilder = (ReferenceWithMetaBuilder<?>) builder;
            return Optional.ofNullable(refBuilder.getReference()).map(RosettaModelObjectBuilder::hasData).orElse(false);
        }
        return false;
    }

    private boolean isBasicReferenceWithMetaContainingReference(RosettaModelObjectBuilder builder) {
        if (isBasicReferenceWithMetaBuilder(builder)) {
            BasicReferenceWithMetaBuilder<?> refBuilder = (BasicReferenceWithMetaBuilder<?>) builder;
            return Optional.ofNullable(refBuilder.getReference()).map(RosettaModelObjectBuilder::hasData).orElse(false);
        }
        return false;
    }

    private boolean isReferenceWithMetaBuilder(RosettaModelObjectBuilder builder) {
    	return builder instanceof ReferenceWithMetaBuilder && ((ReferenceWithMetaBuilder) builder).getReference() != null;
	}

    private boolean isBasicReferenceWithMetaBuilder(RosettaModelObjectBuilder builder) {
        return builder instanceof BasicReferenceWithMetaBuilder && ((BasicReferenceWithMetaBuilder) builder).getReference() != null;
    }

	private boolean isGlobalKeyFieldsBuilder(RosettaModelObjectBuilder builder) {
        return builder instanceof GlobalKeyFields.GlobalKeyFieldsBuilder;
    }

    private boolean isTemplateFieldsBuilder(RosettaModelObjectBuilder builder) {
        return builder instanceof TemplateFields.TemplateFieldsBuilder;
    }

    private boolean metaContains(AttributeMeta[] metas, AttributeMeta attributeMeta) {
        return Arrays.stream(metas).anyMatch(m -> m == attributeMeta);
    }

    private static class Result {
        private final boolean includeInHash;
        private final boolean continueProcessing;

        public Result(boolean includeInHash, boolean continueProcessing) {
            this.includeInHash = includeInHash;
            this.continueProcessing = continueProcessing;
        }

        @Override
        public String toString() {
            return "Result{" +
                    "includeInHash=" + includeInHash +
                    ", continueProcessing=" + continueProcessing +
                    '}';
        }
    }
}
