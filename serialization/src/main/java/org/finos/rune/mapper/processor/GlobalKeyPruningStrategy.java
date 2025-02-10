package org.finos.rune.mapper.processor;

import com.rosetta.model.lib.GlobalKey;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.meta.FieldWithMeta;
import com.rosetta.model.lib.meta.GlobalKeyFields;

import java.util.Set;

public class GlobalKeyPruningStrategy implements PruningStrategy {
    private final Set<GlobalReferenceRecord> globalReferences;

    public GlobalKeyPruningStrategy(Set<GlobalReferenceRecord> globalReferences) {
        this.globalReferences = globalReferences;
    }

    public void prune(RosettaModelObjectBuilder builder) {
        if (builder instanceof GlobalKey.GlobalKeyBuilder) {
            GlobalKey.GlobalKeyBuilder globalKeyBuilder = (GlobalKey.GlobalKeyBuilder) builder;
            GlobalKeyFields.GlobalKeyFieldsBuilder globalKeyFields = globalKeyBuilder.getMeta();
            String globalKey = globalKeyFields.getGlobalKey();
            if (globalKey != null) {
                GlobalReferenceRecord globalReferenceRecord = new GlobalReferenceRecord(getType(builder), globalKey);
                if (!globalReferences.contains(globalReferenceRecord)) {
                    globalKeyFields.setGlobalKey(null);
                }
            }
        }
    }

    private Class<?> getType(RosettaModelObjectBuilder builder) {
        if (builder instanceof FieldWithMeta.FieldWithMetaBuilder) {
            return ((FieldWithMeta.FieldWithMetaBuilder<?>)builder).getValueType();
        }
        return builder.getType();
    }

}
