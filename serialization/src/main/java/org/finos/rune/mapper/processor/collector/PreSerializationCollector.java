package org.finos.rune.mapper.processor.collector;

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
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.Processor;

import java.util.Collection;
import java.util.List;

/**
 * A processor that applies a set of {@link CollectorStrategy} instances to collect relevant data
 * before serialization. Each strategy extracts information from Rosetta model objects to
 * support downstream processing.
 */
public class PreSerializationCollector implements Processor {
    private final List<CollectorStrategy> collectorStrategies;

    public PreSerializationCollector(CollectorStrategy... collectorStrategies) {
        this.collectorStrategies = Lists.newArrayList(collectorStrategies);
    }

    @Override
    public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path, Class<? extends R> rosettaType, R instance, RosettaModelObject parent, AttributeMeta... metas) {
        for (CollectorStrategy collectorStrategy : collectorStrategies) {
            collectorStrategy.collect(instance);
        }
        return true;
    }

    @Override
    public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path, Class<? extends R> rosettaType, List<? extends R> instances, RosettaModelObject parent, AttributeMeta... metas) {
        if (instances == null) {
            return false;
        }
        boolean result = true;
        for (int i = 0; i < instances.size(); i++) {
            R instance = instances.get(i);
            path = path.withIndex(i);
            result &= processRosetta(path, rosettaType, instance, parent, metas);
        }
        return result;
    }

    @Override
    public <T> void processBasic(RosettaPath path, Class<? extends T> rosettaType, T instance, RosettaModelObject parent, AttributeMeta... metas) {
        //No references on basic types, they exist instead on their corresponding wrapper type
    }

    @Override
    public <T> void processBasic(RosettaPath path, Class<? extends T> rosettaType, Collection<? extends T> instances, RosettaModelObject parent, AttributeMeta... metas) {
        if (instances == null)
            return;
        for (T instance : instances) {
            processBasic(path, rosettaType, instance, parent, metas);
        }
    }

    @Override
    public Report report() {
        throw new UnsupportedOperationException("Report not supported for UnreferencedKeyCollector");
    }
}
