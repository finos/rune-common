package com.regnosys.rosetta.common.serialisation;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rosetta.model.lib.meta.Key;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class RosettaObjectMapperTest {

	@Test
	void testSerialiseBuilder() throws IOException {
		ObjectMapper mapper = RosettaObjectMapper.getNewRosettaObjectMapper();
		
		Key.KeyBuilder k= Key.builder().setScope("TestScope").setKeyValue("KEY");
		
		String serial = mapper.writeValueAsString(k);
		System.out.println(serial);
		
		Key newKey = mapper.readValue(serial, Key.class);
		
		assertEquals(k.build(), newKey);
	}
	
	@Test
	void testSerialiseBuilder2() throws IOException {
		ObjectMapper mapper = RosettaObjectMapper.getNewRosettaObjectMapper();
		
		Key.KeyBuilder k= Key.builder().setScope("TestScope").setKeyValue("KEY");
		
		String serial = mapper.writeValueAsString(k);
		System.out.println(serial);
		
		Key newKey = mapper.readValue(serial, Key.class);
		
		assertEquals(k.build(), newKey);
	}
	
	@Test
	void testSerialiseBuilt() throws IOException {
		ObjectMapper mapper = RosettaObjectMapper.getNewRosettaObjectMapper();
		
		Key k= Key.builder().setScope("TestScope").setKeyValue("KEY").build();
		
		String serial = mapper.writeValueAsString(k);
		System.out.println(serial);
		
		Key newKey = mapper.readValue(serial, Key.class);
		
		assertEquals(k, newKey);
	}

}
