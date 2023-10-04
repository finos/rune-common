package com.regnosys.rosetta.common.serialisation.json;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapper;
import com.rosetta.model.lib.meta.Key;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class RosettaObjectMapperTest {

	@Test
	void testSerialiseBuilder() throws IOException {
		ObjectMapper mapper = RosettaObjectMapper.getOptimizedRosettaJSONMapper();
		
		Key.KeyBuilder k= Key.builder().setScope("TestScope").setKeyValue("KEY");
		
		String serial = mapper.writeValueAsString(k);
		System.out.println(serial);
		
		Key newKey = mapper.readValue(serial, Key.class);
		
		assertEquals(k.build(), newKey);
	}
	
	@Test
	void testSerialiseBuilder2() throws IOException {
		ObjectMapper mapper = RosettaObjectMapper.getOptimizedRosettaJSONMapper();
		
		Key.KeyBuilder k= Key.builder().setScope("TestScope").setKeyValue("KEY");
		
		String serial = mapper.writeValueAsString(k);
		System.out.println(serial);
		
		Key newKey = mapper.readValue(serial, Key.class);
		
		assertEquals(k.build(), newKey);
	}
	
	@Test
	void testSerialiseBuilt() throws IOException {
		ObjectMapper mapper = RosettaObjectMapper.getOptimizedRosettaJSONMapper();
		
		Key k= Key.builder().setScope("TestScope").setKeyValue("KEY").build();
		
		String serial = mapper.writeValueAsString(k);
		System.out.println(serial);
		
		Key newKey = mapper.readValue(serial, Key.class);
		
		assertEquals(k, newKey);
	}

}
