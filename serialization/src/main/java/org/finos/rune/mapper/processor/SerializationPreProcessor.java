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
import org.finos.rune.mapper.processor.collector.*;
import org.finos.rune.mapper.processor.pruner.*;

import java.util.List;
import java.util.Set;

public class SerializationPreProcessor {

    public <T extends RosettaModelObject> T process(T rosettaModelObject) {
        RosettaPath path = RosettaPath.valueOf(rosettaModelObject.getType().getSimpleName());

        // Collect global references and key information
        KeyCollectorStrategy keyCollectorStrategy = new KeyCollectorStrategy();
        List<CollectorStrategy> collectorStrategies = Lists.newArrayList(keyCollectorStrategy);
        PreSerializationCollector keyLookupCollector = new PreSerializationCollector(collectorStrategies);
        rosettaModelObject.process(path, keyLookupCollector);
        KeyLookupService keyLookupService = keyCollectorStrategy.getKeyLookupService();

        RosettaModelObjectBuilder builder = rosettaModelObject.toBuilder();

        // Prune References
        ReferencePruningStrategy referencePruningStrategy = new ReferencePruningStrategy(keyLookupService);
        PreSerializationPruner referencePruning = new PreSerializationPruner(Lists.newArrayList(referencePruningStrategy));
        builder.process(path, referencePruning);

        // Collect global refs after ref pruning
        GlobalReferenceCollectorStrategy globalReferenceCollectorStrategy = new GlobalReferenceCollectorStrategy();
        PreSerializationCollector globalReferenceCollector = new PreSerializationCollector(Lists.newArrayList(globalReferenceCollectorStrategy));
        builder.process(path, globalReferenceCollector);
        Set<GlobalReferenceRecord> globalReferences = globalReferenceCollectorStrategy.getGlobalReferences();


        // Prune keys and attributes
        GlobalKeyPruningStrategy globalKeyPruningStrategy = new GlobalKeyPruningStrategy(globalReferences);
        EmptyAttributePruningStrategy emptyAttributePruningStrategy = new EmptyAttributePruningStrategy();
        List<PruningStrategy> pruningStrategyList = Lists.newArrayList(globalKeyPruningStrategy, emptyAttributePruningStrategy);
        PreSerializationPruner keyAndAttributePruning = new PreSerializationPruner(pruningStrategyList);
        builder.process(path, keyAndAttributePruning);

        return buildAndCast(builder);
    }

    @SuppressWarnings("unchecked")
    private <T extends RosettaModelObject> T buildAndCast(RosettaModelObjectBuilder builder) {
        return (T) builder.build();
    }
}
