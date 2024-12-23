package org.finos.rune.serialization;

import com.rosetta.model.lib.RosettaModelObject;

// JacksonRuneJsonSerializer (impl) new up
public interface RuneJsonSerializer {

    <T extends RosettaModelObject> String toJson(T runeObject);

    <T extends RosettaModelObject> T fromJson(String runeJson, Class<T> type);

}
