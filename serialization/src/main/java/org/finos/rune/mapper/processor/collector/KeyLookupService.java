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

public class KeyLookupService {
    private final Map<KeyRecord, Object> globalKeyToValueObjectMap;
    private final Map<KeyRecord, Object> externalKeyToValueObjectMap;
    private final Map<KeyRecord, Object> addressToValueObjectMap;

    public KeyLookupService(Map<KeyRecord, Object> globalKeyToValueObjectMap,
                            Map<KeyRecord, Object> externalKeyToValueObjectMap,
                            Map<KeyRecord, Object> addressToValueObjectMap) {
        this.globalKeyToValueObjectMap = globalKeyToValueObjectMap;
        this.externalKeyToValueObjectMap = externalKeyToValueObjectMap;
        this.addressToValueObjectMap = addressToValueObjectMap;
    }

    public Object getReferencedObject(KeyType keyType, Class<?> keyOnType, String keyReferenceValue) {
        switch (keyType) {
            case GLOBAL_KEY:
                return globalKeyToValueObjectMap.get(new KeyRecord(keyOnType, keyReferenceValue));
            case EXTERNAL_KEY:
                return externalKeyToValueObjectMap.get(new KeyRecord(keyOnType, keyReferenceValue));
            case ADDRESS:
                return addressToValueObjectMap.get(new KeyRecord(keyOnType, keyReferenceValue));
            default:
                throw new IllegalArgumentException("Unknown key type: " + keyType);
        }
    }

    public enum KeyType {
        GLOBAL_KEY, EXTERNAL_KEY, ADDRESS
    }
}
