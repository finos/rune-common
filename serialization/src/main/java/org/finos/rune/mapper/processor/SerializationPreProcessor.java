package org.finos.rune.mapper.processor;

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

import java.util.List;

public class SerializationPreProcessor {

    public <T extends RosettaModelObject> T process(T rosettaModelObject) {
        RosettaPath path = RosettaPath.valueOf(rosettaModelObject.getType().getSimpleName());

        GlobalReferenceCollectorStrategy globalReferenceCollectorStrategy = new GlobalReferenceCollectorStrategy();
        List<CollectorStrategy> collectorStrategies = Lists.newArrayList(globalReferenceCollectorStrategy);
        MetaCollector metaCollector = new MetaCollector(collectorStrategies);
        rosettaModelObject.process(path, metaCollector);

        GlobalKeyPruningStrategy globalKeyPruningStrategy = new GlobalKeyPruningStrategy(globalReferenceCollectorStrategy.getGlobalReferences());
        EmptyAttributePruningStrategy emptyAttributePruningStrategy = new EmptyAttributePruningStrategy();
        List<PruningStrategy> pruningStrategyList = Lists.newArrayList(globalKeyPruningStrategy, emptyAttributePruningStrategy);
        PreSerializationPruner preSerializationPruner = new PreSerializationPruner(pruningStrategyList);
        RosettaModelObjectBuilder builder = rosettaModelObject.toBuilder();
        builder.process(path, preSerializationPruner);

        return buildAndCast(builder);
    }

    @SuppressWarnings("unchecked")
    private <T extends RosettaModelObject> T buildAndCast(RosettaModelObjectBuilder builder) {
        return (T) builder.build();
    }
}
