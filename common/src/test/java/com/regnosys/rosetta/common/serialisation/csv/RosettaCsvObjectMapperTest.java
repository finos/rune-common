package com.regnosys.rosetta.common.serialisation.csv;

import com.regnosys.rosetta.common.serialisation.RosettaCsvObjectMapper;
import com.rosetta.model.lib.meta.Key;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RosettaCsvObjectMapperTest {

    @Test
    void testCsvMapperBuilder() throws IOException {
        RosettaCsvObjectMapper csvObjectMapper = RosettaCsvObjectMapper.createCsvObjectMapper();
        Key.KeyBuilder key = Key.builder().setScope("TestScope").setKeyValue("TestKeyValue");

        String serializedKey = csvObjectMapper.writeCsv(key);
        Key newKey = csvObjectMapper.readValue(serializedKey, Key.class);

        assertEquals(key.build(), newKey);
    }
}
