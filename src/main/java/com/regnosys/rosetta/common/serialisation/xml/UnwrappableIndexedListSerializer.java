package com.regnosys.rosetta.common.serialisation.xml;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.impl.PropertySerializerMap;
import com.fasterxml.jackson.databind.ser.std.AsArraySerializerBase;
import com.fasterxml.jackson.databind.util.NameTransformer;

import java.io.IOException;
import java.util.List;

public class UnwrappableIndexedListSerializer extends AsArraySerializerBase<List<?>> {
    private static final long serialVersionUID = 1L;

    public UnwrappableIndexedListSerializer(JavaType elemType, boolean staticTyping, TypeSerializer vts,
                                            JsonSerializer<Object> valueSerializer) {
        super(List.class, elemType, staticTyping, vts, valueSerializer);
    }

    public UnwrappableIndexedListSerializer(UnwrappableIndexedListSerializer src,
                                            BeanProperty property, com.fasterxml.jackson.databind.jsontype.TypeSerializer vts, JsonSerializer<?> valueSerializer,
                                            Boolean unwrapSingle) {
        super(src, property, vts, valueSerializer, unwrapSingle);
    }

    @Override
    public UnwrappingIndexedListSerialiser unwrappingSerializer(NameTransformer t) {
        return new UnwrappingIndexedListSerialiser(_elementType, _staticTyping, _valueTypeSerializer, _elementSerializer, t);
    }

    @Override
    public UnwrappableIndexedListSerializer withResolved(BeanProperty property,
                                              com.fasterxml.jackson.databind.jsontype.TypeSerializer vts, JsonSerializer<?> elementSerializer,
                                              Boolean unwrapSingle) {
        return new UnwrappableIndexedListSerializer(this, property, vts, elementSerializer, unwrapSingle);
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
    public UnwrappableIndexedListSerializer _withValueTypeSerializer(com.fasterxml.jackson.databind.jsontype.TypeSerializer vts) {
        return new UnwrappableIndexedListSerializer(this,
                _property, vts, _elementSerializer, _unwrapSingle);
    }

    @Override
    public final void serialize(List<?> value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        final int len = value.size();
        if (len == 1) {
            if (((_unwrapSingle == null) &&
                    provider.isEnabled(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED))
                    || (_unwrapSingle == Boolean.TRUE)) {
                serializeContents(value, gen, provider);
                return;
            }
        }
        gen.writeStartArray(value, len);
        serializeContents(value, gen, provider);
        gen.writeEndArray();
    }

    @Override
    public void serializeContents(List<?> value, JsonGenerator g, SerializerProvider provider)
            throws IOException {
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
            throws IOException {
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
            throws IOException {
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
