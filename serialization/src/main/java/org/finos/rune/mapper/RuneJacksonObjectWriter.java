package org.finos.rune.mapper;

import com.fasterxml.jackson.core.FormatSchema;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.annotations.RuneDataType;

import java.util.Arrays;

public class RuneJacksonObjectWriter extends ObjectWriter {
    private final ObjectMapper mapper;


    protected RuneJacksonObjectWriter(ObjectMapper mapper, SerializationConfig config, FormatSchema s) {
        super(mapper, config, s);
        this.mapper = mapper;
    }

    public RuneJacksonObjectWriter(ObjectMapper mapper, SerializationConfig config) {
        super(mapper, config);
        this.mapper = mapper;
    }

    protected RuneJacksonObjectWriter(ObjectMapper mapper, SerializationConfig config, JavaType rootType, PrettyPrinter pp) {
        super(mapper, config, rootType, pp);
        this.mapper = mapper;
    }

    @Override
    public String writeValueAsString(Object value) throws JsonProcessingException {
        if (value instanceof RosettaModelObject) {
            RosettaModelObject rosettaModelObject = (RosettaModelObject) value;
            ObjectNode objectNode = addTopLevelMeta(rosettaModelObject);
            return super.writeValueAsString(objectNode);
        }

        return super.writeValueAsString(value);
    }

    private <T extends RosettaModelObject> ObjectNode addTopLevelMeta(T runeObject) {
        Class<? extends RosettaModelObject> runeType = runeObject.getType();
        return Arrays.stream(runeType.getAnnotations())
                .filter(allAnnotations -> allAnnotations.annotationType().equals(RuneDataType.class)).findFirst().map(a -> {
                    RuneDataType runeDataType = (RuneDataType) a;
                    ObjectNode modifiedNode = mapper.valueToTree(runeObject);

                    return modifiedNode.put("@model", runeDataType.model())
                            .put("@type", runeType.getCanonicalName())
                            .put("@version", runeDataType.version());

                }).orElse(mapper.valueToTree(runeObject));
    }
}
