package com.regnosys.rosetta.common.serialisation.json;

/*-
 * ==============
 * Rune Common
 * ==============
 * Copyright (C) 2018 - 2024 REGnosys
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
