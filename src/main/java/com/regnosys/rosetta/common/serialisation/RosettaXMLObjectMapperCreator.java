package com.regnosys.rosetta.common.serialisation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.regnosys.rosetta.common.serialisation.mixin.RosettaXMLModule;
import com.rosetta.util.serialisation.RosettaXMLConfiguration;

public class RosettaXMLObjectMapperCreator extends AbstractRosettaObjectMapperCreator {
    public RosettaXMLObjectMapperCreator(RosettaXMLConfiguration xmlConfiguration, boolean supportNativeEnumValue) {
        super(supportNativeEnumValue, new RosettaXMLModule(xmlConfiguration, supportNativeEnumValue));
    }

    @Override
    protected ObjectMapper createBaseObjectMapper() {
        return new XmlMapper();
    }
}
