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
import com.rosetta.model.lib.path.RosettaPath;
import org.finos.rune.mapper.processor.KeyRecord;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static java.util.Optional.ofNullable;

public class KeyCollectorStrategy implements CollectorStrategy {
    private final Map<KeyRecord, Object> globalKeyToValueObjectMap = new HashMap<>();
    private final Map<KeyRecord, Object> externalKeyToValueObjectMap = new HashMap<>();
    private final Map<KeyRecord, Object> addressToValueObjectMap = new HashMap<>();

    @Override
    public <R extends RosettaModelObject> void collect(RosettaPath path, R instance) {
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
