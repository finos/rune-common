package org.finos.rune.mapper.processor;

import com.rosetta.model.lib.GlobalKey;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
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
        pruneGlobalKeys(rosettaType, builder);
        pruneEmptyAttributes(builder);
        return true;
    }

    @Override
    public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path, Class<R> rosettaType, List<? extends RosettaModelObjectBuilder> builders, RosettaModelObjectBuilder parent, AttributeMeta... metas) {
        return false;
    }

    @Override
    public <T> void processBasic(RosettaPath path, Class<T> rosettaType, T instance, RosettaModelObjectBuilder parent, AttributeMeta... metas) {

    }

    @Override
    public <T> void processBasic(RosettaPath path, Class<T> rosettaType, Collection<? extends T> instances, RosettaModelObjectBuilder parent, AttributeMeta... metas) {

    }

    @Override
    public Report report() {
        throw new UnsupportedOperationException("PreSerializationPruner report not supported");
    }

    private <R extends RosettaModelObject> void pruneGlobalKeys(Class<R> rosettaType, RosettaModelObjectBuilder builder) {
        if (builder instanceof GlobalKey.GlobalKeyBuilder) {
            GlobalKey.GlobalKeyBuilder globalKeyBuilder = (GlobalKey.GlobalKeyBuilder) builder;
            GlobalKeyFields.GlobalKeyFieldsBuilder globalKeyFields = globalKeyBuilder.getMeta();
            String globalKey = globalKeyFields.getGlobalKey();
            if (globalKey != null) {
                GlobalReferenceRecord globalReferenceRecord = new GlobalReferenceRecord(rosettaType, globalKey);
                if (!globalReferences.contains(globalReferenceRecord)) {
                    globalKeyFields.setGlobalKey(null);
                }
            }
        }
    }

    private void pruneEmptyAttributes(RosettaModelObjectBuilder builder) {
        builder.prune();
    }
}
