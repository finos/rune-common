package org.finos.rune.mapper.processor.collector;

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

import com.rosetta.model.lib.GlobalKey;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.meta.FieldWithMeta;
import com.rosetta.model.lib.meta.GlobalKeyFields;
import com.rosetta.model.lib.meta.Key;
import org.finos.rune.mapper.processor.KeyRecord;
import org.finos.rune.mapper.processor.pruner.ReferencePruningStrategy;

import java.util.HashMap;
import java.util.Map;

import static java.util.Optional.ofNullable;

/**
 * A strategy implementation for collecting key-related information from instances of {@link RosettaModelObject}.
 * For every keyed object it records how that object's keys relate to one another: which external and global key a
 * given address key belongs to, and which global key a given external key belongs to.
 * <p>
 * These relationships are what the {@link KeyLookupService} exposes to {@link ReferencePruningStrategy} so it can
 * decide that two references on the same holder are redundant - they are when the single definition identified by the
 * higher-precedence reference also owns the lower-precedence key. Precedence runs:
 * <ol>
 *   <li>ADDRESS (highest precedence)</li>
 *   <li>EXTERNAL</li>
 *   <li>GLOBAL (lowest precedence)</li>
 * </ol>
 * <p>
 * The maps are keyed by the address and external keys, which are unique to a definition within a document. Crucially
 * this means a resolved/inlined copy of a reference - which carries the same global key as the genuine definition but
 * different content, and frequently no external key at all - cannot overwrite the genuine relationship. That makes
 * de-duplication immune to global-key collisions, so it behaves identically whether or not a reference still has its
 * value inlined (which is what keeps serialisation idempotent across a read/write round-trip).
 * </p>
 *
 * @see RosettaModelObject
 * @see GlobalKey
 * @see KeyRecord
 * @see KeyLookupService
 * @see ReferencePruningStrategy
 */
public class KeyCollectorStrategy implements CollectorStrategy {
    private final Map<KeyRecord, String> addressKeyToOwnExternalKey = new HashMap<>();
    private final Map<KeyRecord, String> addressKeyToOwnGlobalKey = new HashMap<>();
    private final Map<KeyRecord, String> externalKeyToOwnGlobalKey = new HashMap<>();

    @Override
    public void collect(RosettaModelObject instance) {
        if (instance instanceof GlobalKey) {
            GlobalKey globalKey = (GlobalKey) instance;
            Class<?> valueClass = getValueType(instance);
            GlobalKeyFields meta = globalKey.getMeta();
            if (valueClass == null || meta == null) {
                return;
            }
            String ownGlobalKey = meta.getGlobalKey();
            String ownExternalKey = meta.getExternalKey();

            if (ownExternalKey != null && ownGlobalKey != null) {
                externalKeyToOwnGlobalKey.put(new KeyRecord(valueClass, ownExternalKey), ownGlobalKey);
            }

            ofNullable(meta.getKey()).ifPresent(keys -> {
                for (Key key : keys) {
                    String addressKey = key.getKeyValue();
                    if (addressKey == null) {
                        continue;
                    }
                    if (ownExternalKey != null) {
                        addressKeyToOwnExternalKey.put(new KeyRecord(valueClass, addressKey), ownExternalKey);
                    }
                    if (ownGlobalKey != null) {
                        addressKeyToOwnGlobalKey.put(new KeyRecord(valueClass, addressKey), ownGlobalKey);
                    }
                }
            });
        }
    }

    public KeyLookupService getKeyLookupService() {
        return new KeyLookupService(addressKeyToOwnExternalKey, addressKeyToOwnGlobalKey, externalKeyToOwnGlobalKey);
    }

    private Class<?> getValueType(RosettaModelObject builder) {
        if (builder instanceof FieldWithMeta) {
            return ((FieldWithMeta<?>) builder).getValueType();
        } else
            return builder.getType();
    }
}
