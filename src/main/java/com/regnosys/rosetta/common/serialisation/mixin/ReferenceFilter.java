package com.regnosys.rosetta.common.serialisation.mixin;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.rosetta.model.lib.meta.ReferenceWithMeta;

//This class serves to ensure that the value of a reference doesn't get serialized if the
//reference or global key field is populated
public class ReferenceFilter extends SimpleBeanPropertyFilter {

    @Override
    public void serializeAsField(Object pojo, JsonGenerator jgen, SerializerProvider provider,
                                 PropertyWriter writer) throws Exception {
        if (!filterOut(pojo, writer.getName())) {
            writer.serializeAsField(pojo, jgen, provider);
        }
    }

    @Override
    public void serializeAsField(Object bean, JsonGenerator jgen, SerializerProvider provider,
                                 BeanPropertyWriter writer) throws Exception {
        if (!filterOut(bean, writer.getName())) {
            writer.serializeAsField(bean, jgen, provider);
        }
    }

    private boolean filterOut(Object pojo, String name) {
        if (!name.equals("value")) return false;
        if (pojo instanceof ReferenceWithMeta) {
            return hasReference((ReferenceWithMeta<?>) pojo);
        }
        return false;
    }

    private boolean hasReference(ReferenceWithMeta<?> pojo) {
        return pojo.getGlobalReference() != null || (pojo.getReference() != null && pojo.getReference().getReference() != null);
    }
}
