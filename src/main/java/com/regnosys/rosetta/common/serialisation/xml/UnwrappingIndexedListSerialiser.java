package com.regnosys.rosetta.common.serialisation.xml;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.impl.PropertySerializerMap;
import com.fasterxml.jackson.databind.util.NameTransformer;

import java.io.IOException;
import java.util.List;

public class UnwrappingIndexedListSerialiser extends UnwrappingAsArraySerialiserBase<List<?>> {
    private static final long serialVersionUID = 1L;

    public UnwrappingIndexedListSerialiser(JavaType elemType, boolean staticTyping, TypeSerializer vts,
                                 JsonSerializer<Object> valueSerializer, NameTransformer nameTransformer)
    {
        super(List.class, elemType, staticTyping, vts, valueSerializer, nameTransformer);
    }

    public UnwrappingIndexedListSerialiser(UnwrappingIndexedListSerialiser src,
                                 BeanProperty property, TypeSerializer vts, JsonSerializer<?> valueSerializer) {
        super(src, property, vts, valueSerializer);
    }

    @Override
    public boolean isUnwrappingSerializer() {
        return true;
    }

    @Override
    public UnwrappingIndexedListSerialiser withResolved(BeanProperty property,
                                              TypeSerializer vts, JsonSerializer<?> elementSerializer) {
        return new UnwrappingIndexedListSerialiser(this, property, vts, elementSerializer);
    }

    /*
    /**********************************************************
    /* Accessors
    /**********************************************************
     */

    @Override
    public boolean isEmpty(SerializerProvider prov, List<?> value) {
        return value.isEmpty();
    }

    @Override
    public boolean hasSingleElement(List<?> value) {
        return (value.size() == 1);
    }

    @Override
    public UnwrappingIndexedListSerialiser _withValueTypeSerializer(TypeSerializer vts) {
        return new UnwrappingIndexedListSerialiser(this,
                _property, vts, _elementSerializer);
    }

    @Override
    public final void serialize(List<?> value, JsonGenerator gen, SerializerProvider provider)
            throws IOException
    {
        serializeContents(value, gen, provider);
    }

    @Override
    public void serializeContents(List<?> value, JsonGenerator g, SerializerProvider provider)
            throws IOException
    {
        if (_elementSerializer != null) {
            serializeContentsUsing(value, g, provider, _elementSerializer);
            return;
        }
        if (_valueTypeSerializer != null) {
            serializeTypedContents(value, g, provider);
            return;
        }
        final int len = value.size();
        if (len == 0) {
            return;
        }
        int i = 0;
        try {
            PropertySerializerMap serializers = _dynamicSerializers;
            for (; i < len; ++i) {
                Object elem = value.get(i);
                if (elem == null) {
                    provider.defaultSerializeNull(g);
                } else {
                    Class<?> cc = elem.getClass();
                    JsonSerializer<Object> serializer = serializers.serializerFor(cc);
                    if (serializer == null) {
                        // To fix [JACKSON-508]
                        if (_elementType.hasGenericTypes()) {
                            serializer = _findAndAddDynamic(serializers,
                                    provider.constructSpecializedType(_elementType, cc), provider);
                        } else {
                            serializer = _findAndAddDynamic(serializers, cc, provider);
                        }
                        serializers = _dynamicSerializers;
                    }
                    serializer.serialize(elem, g, provider);
                }
            }
        } catch (Exception e) {
            wrapAndThrow(provider, e, value, i);
        }
    }

    public void serializeContentsUsing(List<?> value, JsonGenerator jgen, SerializerProvider provider,
                                       JsonSerializer<Object> ser)
            throws IOException
    {
        final int len = value.size();
        if (len == 0) {
            return;
        }
        final TypeSerializer typeSer = _valueTypeSerializer;
        for (int i = 0; i < len; ++i) {
            Object elem = value.get(i);
            try {
                if (elem == null) {
                    provider.defaultSerializeNull(jgen);
                } else if (typeSer == null) {
                    ser.serialize(elem, jgen, provider);
                } else {
                    ser.serializeWithType(elem, jgen, provider, typeSer);
                }
            } catch (Exception e) {
                // [JACKSON-55] Need to add reference information
                wrapAndThrow(provider, e, value, i);
            }
        }
    }

    public void serializeTypedContents(List<?> value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException
    {
        final int len = value.size();
        if (len == 0) {
            return;
        }
        int i = 0;
        try {
            final TypeSerializer typeSer = _valueTypeSerializer;
            PropertySerializerMap serializers = _dynamicSerializers;
            for (; i < len; ++i) {
                Object elem = value.get(i);
                if (elem == null) {
                    provider.defaultSerializeNull(jgen);
                } else {
                    Class<?> cc = elem.getClass();
                    JsonSerializer<Object> serializer = serializers.serializerFor(cc);
                    if (serializer == null) {
                        // To fix [JACKSON-508]
                        if (_elementType.hasGenericTypes()) {
                            serializer = _findAndAddDynamic(serializers,
                                    provider.constructSpecializedType(_elementType, cc), provider);
                        } else {
                            serializer = _findAndAddDynamic(serializers, cc, provider);
                        }
                        serializers = _dynamicSerializers;
                    }
                    serializer.serializeWithType(elem, jgen, provider, typeSer);
                }
            }
        } catch (Exception e) {
            wrapAndThrow(provider, e, value, i);
        }
    }
}
