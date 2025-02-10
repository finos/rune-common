package org.finos.rune.mapper.processor;

import com.rosetta.model.lib.GlobalKey;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.meta.FieldWithMeta;
import com.rosetta.model.lib.meta.GlobalKeyFields;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.BuilderProcessor;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class PreSerializationPruner implements BuilderProcessor {
    private final Set<GlobalReferenceRecord> globalReferences;

    public PreSerializationPruner(Set<GlobalReferenceRecord> globalReferences) {
        this.globalReferences = globalReferences;
    }

    @Override
    public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path, Class<R> rosettaType, RosettaModelObjectBuilder builder, RosettaModelObjectBuilder parent, AttributeMeta... metas) {
        if (builder != null) {
            pruneGlobalKeys(builder);
            pruneEmptyAttributes(builder);
            return true;
        }
        return false;
    }

    @Override
    public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path, Class<R> rosettaType, List<? extends RosettaModelObjectBuilder> builders, RosettaModelObjectBuilder parent, AttributeMeta... metas) {
        if (builders == null) {
            return false;
        }
        boolean result = true;
        for (int i = 0; i < builders.size(); i++) {
            RosettaModelObjectBuilder builder = builders.get(i);
            path = path.withIndex(i);
            result &= processRosetta(path, rosettaType, builder, parent, metas);
        }
        return result;
    }

    @Override
    public <T> void processBasic(RosettaPath path, Class<T> rosettaType, T instance, RosettaModelObjectBuilder parent, AttributeMeta... metas) {
        //No pruning of basic types required
    }

    @Override
    public <T> void processBasic(RosettaPath path, Class<T> rosettaType, Collection<? extends T> instances, RosettaModelObjectBuilder parent, AttributeMeta... metas) {
        if (instances == null)
            return;
        for (T instance : instances) {
            processBasic(path, rosettaType, instance, parent, metas);
        }
    }

    @Override
    public Report report() {
        throw new UnsupportedOperationException("PreSerializationPruner report not supported");
    }

    private void pruneGlobalKeys(RosettaModelObjectBuilder builder) {
        if (builder instanceof GlobalKey.GlobalKeyBuilder) {
            GlobalKey.GlobalKeyBuilder globalKeyBuilder = (GlobalKey.GlobalKeyBuilder) builder;
            GlobalKeyFields.GlobalKeyFieldsBuilder globalKeyFields = globalKeyBuilder.getMeta();
            String globalKey = globalKeyFields.getGlobalKey();
            if (globalKey != null) {
                GlobalReferenceRecord globalReferenceRecord = new GlobalReferenceRecord(getType(builder), globalKey);
                if (!globalReferences.contains(globalReferenceRecord)) {
                    globalKeyFields.setGlobalKey(null);
                }
            }
        }
    }

    private Class<?> getType(RosettaModelObjectBuilder builder) {
        if (builder instanceof FieldWithMeta.FieldWithMetaBuilder) {
            return ((FieldWithMeta.FieldWithMetaBuilder<?>)builder).getValueType();
        }
        return builder.getType();
    }

    private void pruneEmptyAttributes(RosettaModelObjectBuilder builder) {
        builder.prune();
    }
}
