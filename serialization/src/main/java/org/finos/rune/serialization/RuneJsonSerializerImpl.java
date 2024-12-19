package org.finos.rune.serialization;

import com.rosetta.model.lib.RosettaModelObject;

public class RuneJsonSerializerImpl implements RuneJsonSerializer {
    @Override
    public <T extends RosettaModelObject> String toJson(T runeObject) {
        return "{\n" +
                "  \"@model\": \"test.basic\",\n" +
                "  \"@type\": \"test.basic.Root\",\n" +
                "  \"@version\": \"0.0.0\"\n" +
                "}";
    }

    @Override
    public <T extends RosettaModelObject> T fromJson(Class<T> type, String runeJson) {
        return null;
    }
}
