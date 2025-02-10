package org.finos.rune.mapper.processor;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.BuilderProcessor;

import java.util.Collection;
import java.util.List;

public class PreSerializationPruner implements BuilderProcessor {
    private final List<PruningStrategy> pruningStrategies;

    public PreSerializationPruner(List<PruningStrategy> pruningStrategies) {
        this.pruningStrategies = pruningStrategies;
    }

    @Override
    public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path, Class<R> rosettaType, RosettaModelObjectBuilder builder, RosettaModelObjectBuilder parent, AttributeMeta... metas) {
        if (builder != null) {
            for (PruningStrategy pruningStrategy : pruningStrategies) {
                pruningStrategy.prune(builder);
            }
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

}
