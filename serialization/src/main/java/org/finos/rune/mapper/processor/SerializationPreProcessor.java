package org.finos.rune.mapper.processor;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.path.RosettaPath;

import java.util.Set;

public class SerializationPreProcessor {

    public <T extends RosettaModelObject> T process(T rosettaModelObject) {
        RosettaPath path = RosettaPath.valueOf(rosettaModelObject.getType().getSimpleName());
        GlobalReferenceCollector globalReferenceCollector = new GlobalReferenceCollector();
        rosettaModelObject.process(path, globalReferenceCollector);

        Set<GlobalReferenceRecord> globalReferences = globalReferenceCollector.getGlobalReferences();
        PreSerializationPruner preSerializationPruner = new PreSerializationPruner(globalReferences);
        RosettaModelObjectBuilder builder = rosettaModelObject.toBuilder();
        builder.process(path, preSerializationPruner);
        return buildAndCast(builder);
    }

    @SuppressWarnings("unchecked")
    private <T extends RosettaModelObject> T buildAndCast(RosettaModelObjectBuilder builder) {
        return (T) builder.build();
    }
}
