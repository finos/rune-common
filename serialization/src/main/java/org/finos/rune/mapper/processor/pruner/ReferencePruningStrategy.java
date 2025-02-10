package org.finos.rune.mapper.processor.pruner;

import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.meta.ReferenceWithMeta;
import org.finos.rune.mapper.processor.collector.KeyLookupService;

public class ReferencePruningStrategy implements PruningStrategy {
    private final KeyLookupService keyLookupService;

    public ReferencePruningStrategy(KeyLookupService keyLookupService) {
        this.keyLookupService = keyLookupService;
    }

    @Override
    public void prune(RosettaModelObjectBuilder builder) {
        if (builder instanceof ReferenceWithMeta.ReferenceWithMetaBuilder) {
            ReferenceWithMeta.ReferenceWithMetaBuilder<?> referenceWithMetaBuilder = (ReferenceWithMeta.ReferenceWithMetaBuilder<?>) builder;
            Class<?> referenceValueType = referenceWithMetaBuilder.getValueType();

            if (referenceWithMetaBuilder.getExternalReference() != null) {
                String externalReference = referenceWithMetaBuilder.getExternalReference();
                boolean higherPrecedenceKeyExists = keyLookupService.higherPrecedenceKeyExists(KeyLookupService.KeyType.EXTERNAL_KEY, referenceValueType, externalReference);
                if (higherPrecedenceKeyExists) {
                    referenceWithMetaBuilder.setExternalReference(null);
                }
            }

            if (referenceWithMetaBuilder.getReference() != null) {
                String globalReference = referenceWithMetaBuilder.getGlobalReference();
                boolean higherPrecedenceKeyExists = keyLookupService.higherPrecedenceKeyExists(KeyLookupService.KeyType.GLOBAL_KEY, referenceValueType, globalReference);
                if (higherPrecedenceKeyExists) {
                    referenceWithMetaBuilder.setGlobalReference(null);
                }
            }
        }
    }


}
