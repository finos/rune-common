package org.finos.rune.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rosetta.model.lib.RosettaModelObject;
import org.finos.rune.mapper.RuneJacksonObjectMapper;

public class RuneJacksonJsonSerializer implements RuneJsonSerializer {

    private final RuneJacksonObjectMapper objectMapper;

    public RuneJacksonJsonSerializer() {
        objectMapper = new RuneJacksonObjectMapper();
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @Override
    public <T extends RosettaModelObject> String toJson(T runeObject) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(runeObject);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T extends RosettaModelObject> T fromJson(String runeJson, Class<T> type) {
        try {
            return objectMapper.readValue(runeJson, type);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
