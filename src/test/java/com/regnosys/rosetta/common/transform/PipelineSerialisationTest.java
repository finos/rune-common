package com.regnosys.rosetta.common.transform;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PipelineSerialisationTest {

  private final ObjectMapper objectMapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_ABSENT);

  @Test
  void pipelineProjectionEmirTradeValid() throws IOException {
    assertPipelinesAreValid("test-pack-serialisation-test/pipeline-projection-emir-trade.json");
  }

  @Test
  void pipelineReportEmirTradeValid() throws IOException {
    assertPipelinesAreValid("test-pack-serialisation-test/pipeline-report-emir-trade.json");
  }

  @Test
  void pipelineTranslateFpmlValid() throws IOException {
    assertPipelinesAreValid("test-pack-serialisation-test/pipeline-translate-fpml.json");
  }

  private void assertPipelinesAreValid(String resourceName) throws IOException {
    String expected = Resources.toString(Resources.getResource(resourceName), StandardCharsets.UTF_8);
    PipelineModel pipeline = objectMapper.readValue(expected, PipelineModel.class);
    String actual = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(pipeline);
    assertEquals(expected, actual);
  }
}
