package org.finos.rune.serialization;

import com.rosetta.model.lib.RosettaModelObject;

public interface RuneSerializer {

    <T extends RosettaModelObject> String toJson(T runeObject);

    <T extends RosettaModelObject> T fromJson(Class<T> type, String runeJson);

}
