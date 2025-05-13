package com.regnosys.rosetta.common.serialisation.xml;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.List;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.Test;

public class XMLTest {

    @JsonDeserialize(builder = InnerPojo.Builder.class)
    public static class InnerPojo {
        
        @JacksonXmlProperty(localName = "Item")
        @JacksonXmlElementWrapper(useWrapping = true, localName = "Items")
        private List<String> items;

        private InnerPojo(Builder builder) {
            this.items = builder.items;
        }

        public List<String> getItems() {
            return items;
        }

        public static Builder builder() {
            return new Builder();
        }

        @JsonPOJOBuilder(withPrefix = "")
        public static class Builder {
            @JacksonXmlProperty(localName = "Item")
            @JacksonXmlElementWrapper(useWrapping = false, localName = "Items")
            private List<String> items;

            public Builder items(List<String> items) {
                this.items = items;
                return this;
            }

            @JacksonXmlProperty(localName = "InnerPojo")
            public InnerPojo build() {
                return new InnerPojo(this);
            }
        }
    }
    
    @Test
    void test() throws Exception {
        String xmlString = "<InnerPojo><Item>Value1</Item><Item>Value2</Item></InnerPojo>";
    
        XmlMapper xmlMapper = new XmlMapper();
        InnerPojo deserializedPojo = xmlMapper.readValue(xmlString, InnerPojo.class);
    
        assert deserializedPojo != null;
        assert deserializedPojo.getItems().size() == 2;
        assert deserializedPojo.getItems().get(0).equals("Value1");
        assert deserializedPojo.getItems().get(1).equals("Value2");
    }
}
