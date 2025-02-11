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

        RosettaModelObjectBuilder builder = rosettaModelObject.toBuilder();

        // Collect key information for all key types
        KeyLookupService keyLookupService = getKeyInformationForAllKeyTypes(builder, path);

        // Prune References
        pruneDuplicateReferences(keyLookupService, builder, path);

        // Collect global references has to be done after ref pruning as global references can be pruned
        Set<GlobalReferenceRecord> globalReferences = getSetOfAllGlobalReferences(builder, path);

        // Prune global keys that no longer have existing references and prune empty attributes
        pruneGlobalKeysAndEmptyAttributes(globalReferences, builder, path);

        return buildAndCast(builder);
    }

    private void pruneGlobalKeysAndEmptyAttributes(Set<GlobalReferenceRecord> globalReferences, RosettaModelObjectBuilder builder, RosettaPath path) {
        GlobalKeyPruningStrategy globalKeyPruningStrategy = new GlobalKeyPruningStrategy(globalReferences);
        List<PruningStrategy> pruningStrategyList = Lists.newArrayList(globalKeyPruningStrategy);
        PreSerializationPruner keyAndAttributePruning = new PreSerializationPruner(pruningStrategyList);
        builder.process(path, keyAndAttributePruning);
        builder.prune();
    }

    private Set<GlobalReferenceRecord> getSetOfAllGlobalReferences(RosettaModelObjectBuilder builder, RosettaPath path) {
        GlobalReferenceCollectorStrategy globalReferenceCollectorStrategy = new GlobalReferenceCollectorStrategy();
        PreSerializationCollector globalReferenceCollector = new PreSerializationCollector(Lists.newArrayList(globalReferenceCollectorStrategy));
        builder.process(path, globalReferenceCollector);
        return globalReferenceCollectorStrategy.getGlobalReferences();
    }

    private void pruneDuplicateReferences(KeyLookupService keyLookupService, RosettaModelObjectBuilder builder, RosettaPath path) {
        ReferencePruningStrategy referencePruningStrategy = new ReferencePruningStrategy(keyLookupService);
        PreSerializationPruner referencePruning = new PreSerializationPruner(Lists.newArrayList(referencePruningStrategy));
        builder.process(path, referencePruning);
    }

    private KeyLookupService getKeyInformationForAllKeyTypes(RosettaModelObjectBuilder builder, RosettaPath path) {
        KeyCollectorStrategy keyCollectorStrategy = new KeyCollectorStrategy();
        List<CollectorStrategy> collectorStrategies = Lists.newArrayList(keyCollectorStrategy);
        PreSerializationCollector keyLookupCollector = new PreSerializationCollector(collectorStrategies);
        builder.process(path, keyLookupCollector);
        return keyCollectorStrategy.getKeyLookupService();
    }

    @SuppressWarnings("unchecked")
    private <T extends RosettaModelObject> T buildAndCast(RosettaModelObjectBuilder builder) {
        return (T) builder.build();
    }
}
