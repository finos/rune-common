package org.finos.rune.mapper.processor.pruner;

import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.meta.ReferenceWithMeta;
import com.rosetta.model.lib.path.RosettaPath;
import org.finos.rune.mapper.processor.collector.KeyLookupService;

import java.util.Optional;

public class ReferencePruningStrategy implements PruningStrategy {
    private final KeyLookupService keyLookupService;

    public ReferencePruningStrategy(KeyLookupService keyLookupService) {
        this.keyLookupService = keyLookupService;
    }

    @Override
    public void prune(RosettaPath path, RosettaModelObjectBuilder builder) {
        if (builder instanceof ReferenceWithMeta.ReferenceWithMetaBuilder) {
            ReferenceWithMeta.ReferenceWithMetaBuilder<?> referenceWithMetaBuilder = (ReferenceWithMeta.ReferenceWithMetaBuilder<?>) builder;
            Class<?> referenceValueType = referenceWithMetaBuilder.getValueType();

            // for each ref (address, external and global) lookup and valueType perform keyLookupService.getReferencedObject() and store the value object
            // then check precedence by comparing value object

            Optional<Object> addressReferencedObject = Optional.empty();
            if (referenceWithMetaBuilder.getReference() != null && referenceWithMetaBuilder.getReference().getReference() != null) {
                String addressRef = referenceWithMetaBuilder.getReference().getReference();
                addressReferencedObject = Optional.ofNullable(keyLookupService.getReferencedObject(KeyLookupService.KeyType.ADDRESS, referenceValueType, addressRef));
            }

            Optional<Object> externalReferencedObject = Optional.empty();
            if (referenceWithMetaBuilder.getExternalReference() != null) {
                String externalReference = referenceWithMetaBuilder.getExternalReference();
                externalReferencedObject = Optional.ofNullable(keyLookupService.getReferencedObject(KeyLookupService.KeyType.EXTERNAL_KEY, referenceValueType, externalReference));

                if (externalReferencedObject.isPresent() && addressReferencedObject.isPresent()
                        && externalReferencedObject.get().equals(addressReferencedObject.get())) {
                    referenceWithMetaBuilder.setExternalReference(null);
                }
            }

            Optional<Object> globalReferencedObject;
            if (referenceWithMetaBuilder.getGlobalReference() != null) {
                String globalReference = referenceWithMetaBuilder.getGlobalReference();
                globalReferencedObject = Optional.ofNullable(keyLookupService.getReferencedObject(KeyLookupService.KeyType.GLOBAL_KEY, referenceValueType, globalReference));

                if (globalReferencedObject.isPresent()) {
                    if (addressReferencedObject.isPresent() &&  addressReferencedObject.get().equals(globalReferencedObject.get())) {
                        referenceWithMetaBuilder.setGlobalReference(null);
                    }
                    if (externalReferencedObject.isPresent() && externalReferencedObject.get().equals(globalReferencedObject.get())) {
                        referenceWithMetaBuilder.setGlobalReference(null);
                    }
                }
            }
        }
    }


}
