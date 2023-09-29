package com.regnosys.rosetta.common.serialisation.mixin;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.ser.impl.AttributePropertyWriter;
import com.fasterxml.jackson.databind.util.Annotations;

public class ConstantAttributePropertyWriter extends AttributePropertyWriter {
    private final String value;
    public ConstantAttributePropertyWriter(String attrName, BeanPropertyDefinition propDef, Annotations contextAnnotations, JavaType declaredType, final String value) {
        super(attrName, propDef, contextAnnotations, declaredType);
        this.value = value;
    }

    @Override
    protected Object value(Object bean, JsonGenerator jgen, SerializerProvider prov) throws Exception {
        return value;
    }
}
