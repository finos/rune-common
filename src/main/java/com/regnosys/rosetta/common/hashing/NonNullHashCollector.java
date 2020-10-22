package com.regnosys.rosetta.common.hashing;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.meta.GlobalKeyFields;
import com.rosetta.model.lib.meta.TemplateFields;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.Processor;

import java.util.Arrays;

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
        if (shouldIncludeInHash(builder, parent, metas)) {
            report.accumulate();
        }
        return true;
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
    private boolean shouldIncludeInHash(RosettaModelObjectBuilder builder, RosettaModelObjectBuilder parent, AttributeMeta[] metas) {
        return builder != null && builder.hasData()
                && !isGlobalKeyFieldsBuilder(builder) // do not include meta folder in hash, however it's contents maybe included
                && !isTemplateFieldsBuilder(builder) // do not include template folder in hash
                && !isTemplateFieldsBuilder(parent) // do not include any fields from template folder in hash -> this should be done by excluding AttributeMeta.TEMPLATE_GLOBAL_REFERENCE
                && (metas.length == 0
                    || (!metaContains(metas, AttributeMeta.GLOBAL_KEY)
                        && !metaContains(metas, AttributeMeta.EXTERNAL_KEY)
                        && !metaContains(metas, AttributeMeta.GLOBAL_KEY_FIELD)));
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

}
