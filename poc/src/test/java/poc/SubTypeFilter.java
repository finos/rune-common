package poc;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;

public class SubTypeFilter extends SimpleBeanPropertyFilter {

    @Override
    public void serializeAsField(Object pojo, JsonGenerator jgen, SerializerProvider provider, PropertyWriter writer) throws Exception {
        String name = writer.getName();
        if (name.equals("@type")) {
            if (pojo.getClass().getSuperclass().toString().equals("java.lang.Object")) {
                writer.serializeAsField(pojo, jgen, provider);
            }

            return;
        }
        super.serializeAsField(pojo, jgen, provider, writer);
    }


}
