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

import org.finos.rune.mapper.processor.KeyRecord;

import java.util.Map;
import java.util.Objects;

/**
 * Answers, for the keyed object that a higher-precedence reference resolves to, whether that same object also carries
 * a given lower-precedence key. This is what {@code ReferencePruningStrategy} needs to decide that two references on
 * the same holder are redundant: they are redundant precisely when the single keyed object identified by the
 * higher-precedence (more specific, unique) key <em>also</em> owns the lower-precedence key value.
 * <p>
 * The lookups are keyed by the address and external keys only. Those keys are unique to a single definition within a
 * document, so - unlike the global key, which a resolved/inlined copy can duplicate with different content - they
 * cannot be clobbered by a redundant copy. Resolving redundancy through them, and reading the definition's own global
 * (or external) key, is therefore immune to the global-key collisions that a value-identity comparison suffers from.
 */
public class KeyLookupService {
    private final Map<KeyRecord, String> addressKeyToOwnExternalKey;
    private final Map<KeyRecord, String> addressKeyToOwnGlobalKey;
    private final Map<KeyRecord, String> externalKeyToOwnGlobalKey;

    public KeyLookupService(Map<KeyRecord, String> addressKeyToOwnExternalKey,
                            Map<KeyRecord, String> addressKeyToOwnGlobalKey,
                            Map<KeyRecord, String> externalKeyToOwnGlobalKey) {
        this.addressKeyToOwnExternalKey = addressKeyToOwnExternalKey;
        this.addressKeyToOwnGlobalKey = addressKeyToOwnGlobalKey;
        this.externalKeyToOwnGlobalKey = externalKeyToOwnGlobalKey;
    }

    /**
     * Whether the object identified by {@code addressKey} also carries {@code externalKey} as its own external key,
     * i.e. the address and external references point to the same definition.
     */
    public boolean addressAndExternalShareObject(Class<?> type, String addressKey, String externalKey) {
        return addressKey != null && externalKey != null
                && Objects.equals(addressKeyToOwnExternalKey.get(new KeyRecord(type, addressKey)), externalKey);
    }

    /**
     * Whether the object identified by {@code addressKey} also carries {@code globalKey} as its own global key,
     * i.e. the address and global references point to the same definition.
     */
    public boolean addressAndGlobalShareObject(Class<?> type, String addressKey, String globalKey) {
        return addressKey != null && globalKey != null
                && Objects.equals(addressKeyToOwnGlobalKey.get(new KeyRecord(type, addressKey)), globalKey);
    }

    /**
     * Whether the object identified by {@code externalKey} also carries {@code globalKey} as its own global key,
     * i.e. the external and global references point to the same definition.
     */
    public boolean externalAndGlobalShareObject(Class<?> type, String externalKey, String globalKey) {
        return externalKey != null && globalKey != null
                && Objects.equals(externalKeyToOwnGlobalKey.get(new KeyRecord(type, externalKey)), globalKey);
    }
}
