package com.regnosys.rosetta.common.transform;

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
