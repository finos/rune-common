package org.finos.rune.mapper.processor.collector;

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
