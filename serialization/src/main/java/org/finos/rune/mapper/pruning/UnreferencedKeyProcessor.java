package org.finos.rune.mapper.pruning;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.path.RosettaPath;

import java.util.Set;

public class UnreferencedKeyProcessor {

    public <T extends RosettaModelObject> T process(T rosettaModelObject) {
        RosettaPath path = RosettaPath.valueOf(rosettaModelObject.getType().getSimpleName());
        GlobalReferenceCollector globalReferenceCollector = new GlobalReferenceCollector();
        rosettaModelObject.process(path, globalReferenceCollector);

        Set<GlobalReferenceRecord> globalReferences = globalReferenceCollector.getGlobalReferences();
        GlobalKeyPruner globalKeyPruner = new GlobalKeyPruner(globalReferences);
        RosettaModelObjectBuilder builder = rosettaModelObject.toBuilder();
        builder.process(path, globalKeyPruner);
        return buildAndCast(builder);
    }

    @SuppressWarnings("unchecked")
    private <T extends RosettaModelObject> T buildAndCast(RosettaModelObjectBuilder builder) {
        return (T) builder.build();
    }
}
