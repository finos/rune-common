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
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * A strategy implementation for collecting key-related information from instances of {@link RosettaModelObject}.
 * This class processes instances of {@link GlobalKey} and extracts global, external, and address keys from the
 * associated metadata, storing the corresponding values in different maps based on the type of key.
 * <p>
 * The strategy creates and maintains three separate mappings:
 * <ul>
 *   <li>{@link #globalKeyToValueObjectMap}: Maps global keys to their associated values.</li>
 *   <li>{@link #externalKeyToValueObjectMap}: Maps external keys to their associated values.</li>
 *   <li>{@link #addressToValueObjectMap}: Maps address keys to their associated values.</li>
 * </ul>
 * These mappings are used by the {@link KeyLookupService} in {@link ReferencePruningStrategy} to resolve
 * references and ensure proper reference precedence during pruning operations. The collected global key information
 * is used to compare and clear redundant references based on precedence:
 * <ol>
 *   <li>ADDRESS (highest precedence)</li>
 *   <li>EXTERNAL</li>
 *   <li>GLOBAL (lowest precedence)</li>
 * </ol>
 * <p>
 * The collected key-value mappings help ensure that only the most relevant references (higher precedence) are kept
 * while lower-precedence references pointing to the same object are removed.
 * </p>
 *
 * @see RosettaModelObject
 * @see GlobalKey
 * @see KeyRecord
 * @see KeyLookupService
 * @see ReferencePruningStrategy
 */
public class KeyCollectorStrategy implements CollectorStrategy {
    private final Map<KeyRecord, Object> globalKeyToValueObjectMap = new HashMap<>();
    private final Map<KeyRecord, Object> externalKeyToValueObjectMap = new HashMap<>();
    private final Map<KeyRecord, Object> addressToValueObjectMap = new HashMap<>();

    @Override
    public void collect(RosettaModelObject instance) {
        if (instance instanceof GlobalKey) {
            GlobalKey globalKey = (GlobalKey) instance;
            Object value = getValue(instance);
            Class<?> valueClass = getValueType(instance);
            if (value != null && valueClass != null) {
                ofNullable(globalKey.getMeta())
                        .map(GlobalKeyFields::getGlobalKey)
                        .ifPresent(gk -> globalKeyToValueObjectMap.put(new KeyRecord(valueClass, gk), value));

                ofNullable(globalKey.getMeta())
                        .map(GlobalKeyFields::getExternalKey)
                        .ifPresent(ek -> externalKeyToValueObjectMap.put(new KeyRecord(valueClass, ek), value));

                ofNullable(globalKey.getMeta())
                        .map(GlobalKeyFields::getKey)
                        .ifPresent(keys ->
                                keys.stream()
                                        .map(Key::getKeyValue)
                                        .filter(Objects::nonNull)
                                        .forEach(kv -> addressToValueObjectMap.put(new KeyRecord(valueClass, kv), value))
                        );

            }
        }

    }

    public KeyLookupService getKeyLookupService() {
        return new KeyLookupService(globalKeyToValueObjectMap, externalKeyToValueObjectMap, addressToValueObjectMap);
    }

    private Object getValue(RosettaModelObject instance) {
        if (instance instanceof FieldWithMeta) {
            return ((FieldWithMeta<?>) instance).getValue();
        } else
            return instance;
    }

    private Class<?> getValueType(RosettaModelObject builder) {
        if (builder instanceof FieldWithMeta) {
            return ((FieldWithMeta<?>) builder).getValueType();
        } else
            return builder.getType();
    }
}
