package org.finos.rune.mapper.processor;

import com.google.common.collect.Lists;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.path.RosettaPath;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SerializationPreProcessor {

    public <T extends RosettaModelObject> T process(T rosettaModelObject) {
        RosettaPath path = RosettaPath.valueOf(rosettaModelObject.getType().getSimpleName());
        GlobalReferenceCollector globalReferenceCollector = new GlobalReferenceCollector();
        rosettaModelObject.process(path, globalReferenceCollector);
        Set<GlobalReferenceRecord> globalReferences = globalReferenceCollector.getGlobalReferences();

        GlobalKeyPruningStrategy globalKeyPruningStrategy = new GlobalKeyPruningStrategy(globalReferences);
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
