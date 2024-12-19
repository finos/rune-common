package com.regnosys.rosetta.common.transform;

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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestPackSerialisationTest {

  private final ObjectMapper objectMapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_ABSENT);

  @Test
  void testPackProjectionEmirTradeCommodity() throws IOException {
    assertPipelinesAreValid("test-pack-serialisation-test/test-pack-projection-emir-trade-commodity.json");
  }

  @Test
  void testPackReportEmirTradeCommodity() throws IOException {
    assertPipelinesAreValid("test-pack-serialisation-test/test-pack-report-emir-trade-commodity.json");
  }

  @Test
  void testPackTranslateEmirTradeCommodity() throws IOException {
    assertPipelinesAreValid("test-pack-serialisation-test/test-pack-translate-fpml-commodity.json");
  }

  private void assertPipelinesAreValid(String resourceName) throws IOException {
    String expected = Resources.toString(Resources.getResource(resourceName), StandardCharsets.UTF_8);
    TestPackModel testPack = objectMapper.readValue(expected, TestPackModel.class);
    String actual = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(testPack);
    assertEquals(expected, actual);
  }
}
