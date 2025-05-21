package com.regnosys.rosetta.common.serialisation.xml;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

public class JacksonDoubleUnwrappingTest {
    @Test
    void test() throws JsonProcessingException {
        String xml = "<pojo><value>a</value><value>b</value></pojo>";

        XmlMapper mapper = new XmlMapper();
        
        Pojo actual = mapper.readValue(xml, Pojo.class);
        
        Assertions.assertEquals(Arrays.asList("a", "b"), actual.getNested().getValues());
    }
    
    private static class Pojo {
        private Nested nested;
        
        @JsonUnwrapped
        public Nested getNested() {
            return nested;
        }
        public void setNested(Nested nested) {
            this.nested = nested;
        }
    }
    private static class Nested {
        private List<String> values;
        
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "value")
        public List<String> getValues() {
            return values;
        }
        public void setValues(List<String> values) {
            this.values = values;
        }
    }
}
