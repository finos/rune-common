package org.finos.rune.mapper.processor.pruner;

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

import com.google.common.collect.Lists;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.BuilderProcessor;

import java.util.Collection;
import java.util.List;

/**
 * A processor that applies a series of {@link PruningStrategy} implementations to prune
 * unnecessary data from a {@link RosettaModelObjectBuilder} before serialization.
 * This ensures that only relevant data is retained in the final serialized output.
 */
public class PreSerializationPruner implements BuilderProcessor {
    private final List<PruningStrategy> pruningStrategies;

    public PreSerializationPruner(PruningStrategy... pruningStrategies) {
        this.pruningStrategies = Lists.newArrayList(pruningStrategies);
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
