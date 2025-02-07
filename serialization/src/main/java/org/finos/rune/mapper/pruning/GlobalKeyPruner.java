package org.finos.rune.mapper.pruning;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.BuilderProcessor;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class GlobalKeyPruner implements BuilderProcessor {
    private final Set<GlobalReferenceRecord> globalReferences;

    public GlobalKeyPruner(Set<GlobalReferenceRecord> globalReferences) {
        this.globalReferences = globalReferences;
    }

    @Override
    public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path, Class<R> rosettaType, RosettaModelObjectBuilder builder, RosettaModelObjectBuilder parent, AttributeMeta... metas) {
        return false;
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
        throw new UnsupportedOperationException("KeyPruningProcessor report not supported");
    }
}
