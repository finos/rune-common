package org.finos.rune.mapper.processor.pruner;

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

import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.meta.ReferenceWithMeta;
import org.finos.rune.mapper.processor.collector.KeyLookupService;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * A pruning strategy that removes redundant references from a {@link RosettaModelObjectBuilder}.
 * This strategy checks for references of type ADDRESS, EXTERNAL, and GLOBAL, and ensures that lower-precedence
 * references are cleared if they resolve to the same object as a higher-precedence reference.
 *
 * The precedence of references is as follows:
 * 1. ADDRESS (highest precedence)
 * 2. EXTERNAL
 * 3. GLOBAL (lowest precedence)
 */
public class ReferencePruningStrategy implements PruningStrategy {
    private final KeyLookupService keyLookupService;

    public ReferencePruningStrategy(KeyLookupService keyLookupService) {
        this.keyLookupService = keyLookupService;
    }

    @Override
    public void prune(RosettaModelObjectBuilder builder) {
        if (!(builder instanceof ReferenceWithMeta.ReferenceWithMetaBuilder)) {
            return;
        }

        ReferenceWithMeta.ReferenceWithMetaBuilder<?> referenceWithMetaBuilder =
                (ReferenceWithMeta.ReferenceWithMetaBuilder<?>) builder;
        Class<?> referenceValueType = referenceWithMetaBuilder.getValueType();

        Set<Object> referencedObjects = new HashSet<>();

        String reference = referenceWithMetaBuilder.getReference() != null ?
                referenceWithMetaBuilder.getReference().getReference() : null;

        addReferencedObject(referencedObjects,
                reference,
                KeyLookupService.KeyType.ADDRESS, referenceValueType);

        if (isReferencedObjectAlreadyIncluded(referencedObjects, referenceWithMetaBuilder.getExternalReference(),
                KeyLookupService.KeyType.EXTERNAL_KEY, referenceValueType)) {
            referenceWithMetaBuilder.setExternalReference(null);
        } else {
            addReferencedObject(referencedObjects, referenceWithMetaBuilder.getExternalReference(),
                    KeyLookupService.KeyType.EXTERNAL_KEY, referenceValueType);
        }

        if (isReferencedObjectAlreadyIncluded(referencedObjects, referenceWithMetaBuilder.getGlobalReference(),
                KeyLookupService.KeyType.GLOBAL_KEY, referenceValueType)) {
            referenceWithMetaBuilder.setGlobalReference(null);
        }

    }

    private void addReferencedObject(Set<Object> referencedObjects, String reference,
                                     KeyLookupService.KeyType keyType, Class<?> valueType) {
        Optional<Object> referencedObject = lookupReferencedObject(keyType, valueType, reference);
        referencedObject.ifPresent(referencedObjects::add);
    }

    private boolean isReferencedObjectAlreadyIncluded(Set<Object> referencedObjects, String reference,
                                                      KeyLookupService.KeyType keyType, Class<?> valueType) {
        Optional<Object> referencedObject = lookupReferencedObject(keyType, valueType, reference);
        return referencedObject.isPresent() && referencedObjects.contains(referencedObject.get());
    }

    private Optional<Object> lookupReferencedObject(KeyLookupService.KeyType keyType, Class<?> valueType, String reference) {
        return Optional.ofNullable(reference != null ? keyLookupService.getReferencedObject(keyType, valueType, reference) : null);
    }
}
