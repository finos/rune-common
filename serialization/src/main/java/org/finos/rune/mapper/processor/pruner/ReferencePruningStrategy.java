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

/**
 * A pruning strategy that removes redundant references from a {@link RosettaModelObjectBuilder}.
 * This strategy checks for references of type ADDRESS, EXTERNAL, and GLOBAL, and ensures that lower-precedence
 * references are cleared when they resolve to the same definition as a higher-precedence reference on the same holder.
 *
 * The precedence of references is as follows:
 * 1. ADDRESS (highest precedence)
 * 2. EXTERNAL
 * 3. GLOBAL (lowest precedence)
 * <p>
 * Two references are treated as redundant when the single keyed definition identified by the higher-precedence
 * reference also owns the lower-precedence key. This is resolved through the {@link KeyLookupService}, which only
 * indexes the address and external keys - keys unique to a definition - so the decision is unaffected by global-key
 * collisions between a definition and an inlined copy of it.
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
        Class<?> type = referenceWithMetaBuilder.getValueType();

        String addressReference = referenceWithMetaBuilder.getReference() != null ?
                referenceWithMetaBuilder.getReference().getReference() : null;
        String externalReference = referenceWithMetaBuilder.getExternalReference();
        String globalReference = referenceWithMetaBuilder.getGlobalReference();

        // EXTERNAL is redundant when the address reference points at the same definition.
        boolean externalRedundant = keyLookupService.addressAndExternalShareObject(type, addressReference, externalReference);
        if (externalRedundant) {
            referenceWithMetaBuilder.setExternalReference(null);
        }

        // GLOBAL is redundant when a surviving higher-precedence reference points at the same definition: the
        // address reference, or the external reference when it was not itself dropped as redundant.
        boolean globalRedundant = keyLookupService.addressAndGlobalShareObject(type, addressReference, globalReference)
                || (!externalRedundant && keyLookupService.externalAndGlobalShareObject(type, externalReference, globalReference));
        if (globalRedundant) {
            referenceWithMetaBuilder.setGlobalReference(null);
        }

        // If the reference still points to another object via any key type, the inlined value is
        // redundant: the resolved object is serialized at its keyed location, so drop the duplicate body here.
        if (hasReference(referenceWithMetaBuilder)) {
            referenceWithMetaBuilder.setValue(null);
        }
    }

    private boolean hasReference(ReferenceWithMeta.ReferenceWithMetaBuilder<?> builder) {
        return builder.getGlobalReference() != null
                || builder.getExternalReference() != null
                || (builder.getReference() != null && builder.getReference().getReference() != null);
    }
}
