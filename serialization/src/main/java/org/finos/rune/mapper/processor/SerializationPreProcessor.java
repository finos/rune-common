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

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.path.RosettaPath;
import org.finos.rune.mapper.processor.collector.GlobalReferenceCollectorStrategy;
import org.finos.rune.mapper.processor.collector.KeyCollectorStrategy;
import org.finos.rune.mapper.processor.collector.KeyLookupService;
import org.finos.rune.mapper.processor.collector.PreSerializationCollector;
import org.finos.rune.mapper.processor.pruner.GlobalKeyPruningStrategy;
import org.finos.rune.mapper.processor.pruner.PreSerializationPruner;
import org.finos.rune.mapper.processor.pruner.ReferencePruningStrategy;

import java.util.Set;

public class SerializationPreProcessor {

    public <T extends RosettaModelObject> T process(T rosettaModelObject) {
        RosettaPath path = RosettaPath.valueOf(rosettaModelObject.getType().getSimpleName());

        RosettaModelObjectBuilder builder = rosettaModelObject.toBuilder();

        // Collect key information for all key types this is used by the reference de-duplication strategy later
        KeyLookupService keyLookupService = getKeyInformationForAllKeyTypes(builder, path);

        // Prune duplicate References in order of precedence from highest to lowest value: address highest, then external, finally global.
        // Note that to be a duplicate two references must resolve to the same object in the structure
        pruneDuplicateReferences(keyLookupService, builder, path);

        // Collect global references has to be done after ref pruning as global references can be pruned
        Set<GlobalReferenceRecord> globalReferences = getAllGlobalReferences(builder, path);

        // Prune global keys that no longer have existing references and prune empty attributes
        pruneGlobalKeysAndEmptyAttributes(globalReferences, builder, path);

        return buildAndCast(builder);
    }

    private void pruneGlobalKeysAndEmptyAttributes(Set<GlobalReferenceRecord> globalReferences, RosettaModelObjectBuilder builder, RosettaPath path) {
        GlobalKeyPruningStrategy globalKeyPruningStrategy = new GlobalKeyPruningStrategy(globalReferences);
        PreSerializationPruner keyAndAttributePruning = new PreSerializationPruner(globalKeyPruningStrategy);
        builder.process(path, keyAndAttributePruning);
        builder.prune();
    }

    private Set<GlobalReferenceRecord> getAllGlobalReferences(RosettaModelObjectBuilder builder, RosettaPath path) {
        GlobalReferenceCollectorStrategy globalReferenceCollectorStrategy = new GlobalReferenceCollectorStrategy();
        PreSerializationCollector globalReferenceCollector = new PreSerializationCollector(globalReferenceCollectorStrategy);
        builder.process(path, globalReferenceCollector);
        return globalReferenceCollectorStrategy.getGlobalReferences();
    }

    private void pruneDuplicateReferences(KeyLookupService keyLookupService, RosettaModelObjectBuilder builder, RosettaPath path) {
        ReferencePruningStrategy referencePruningStrategy = new ReferencePruningStrategy(keyLookupService);
        PreSerializationPruner referencePruning = new PreSerializationPruner(referencePruningStrategy);
        builder.process(path, referencePruning);
    }

    private KeyLookupService getKeyInformationForAllKeyTypes(RosettaModelObjectBuilder builder, RosettaPath path) {
        KeyCollectorStrategy keyCollectorStrategy = new KeyCollectorStrategy();
        PreSerializationCollector keyLookupCollector = new PreSerializationCollector(keyCollectorStrategy);
        builder.process(path, keyLookupCollector);
        return keyCollectorStrategy.getKeyLookupService();
    }

    @SuppressWarnings("unchecked")
    private <T extends RosettaModelObject> T buildAndCast(RosettaModelObjectBuilder builder) {
        return (T) builder.build();
    }
}
