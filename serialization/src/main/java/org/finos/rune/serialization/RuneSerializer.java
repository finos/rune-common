package org.finos.rune.serialization;

import com.rosetta.model.lib.RosettaModelObject;

public interface RuneSerializer {

    <T extends RosettaModelObject> String serialize(T runeObject);

    <T extends RosettaModelObject> T deserialize(Class<T> type, String runeJson);

}
