package com.regnosys.rosetta.common.serialisation.xml;

/*-
 * ==============
 * Rune Common
 * ==============
 * Copyright (C) 2018 - 2024 REGnosys
 * ==============
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ==============
 */

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.impl.PropertySerializerMap;
import com.fasterxml.jackson.databind.ser.std.AsArraySerializerBase;
import com.fasterxml.jackson.databind.util.NameTransformer;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.List;

// A copy of the final class `IndexedListSerializer`, except for the method `unwrappingSerializer`.
// Also includes substitution capabilities. TODO: find a cleaner way?
public class UnwrappableIndexedListSerializer extends AsArraySerializerBase<List<?>> {
    private static final long serialVersionUID = 1L;

    private SubstitutionMap nextElementSubstitutionMap = null;

    public UnwrappableIndexedListSerializer(JavaType elemType, boolean staticTyping, TypeSerializer vts,
                                            JsonSerializer<Object> valueSerializer) {
        super(List.class, elemType, staticTyping, vts, valueSerializer);
    }

    public UnwrappableIndexedListSerializer(UnwrappableIndexedListSerializer src,
                                            BeanProperty property, com.fasterxml.jackson.databind.jsontype.TypeSerializer vts, JsonSerializer<?> valueSerializer,
                                            Boolean unwrapSingle) {
        super(src, property, vts, valueSerializer, unwrapSingle);
    }

    public void setNextElementSubstitutionMap(SubstitutionMap elementSubstitutionMap) {
        this.nextElementSubstitutionMap = elementSubstitutionMap;
    }

    @Override
    public UnwrappingIndexedListSerializer unwrappingSerializer(NameTransformer t) {
        return new UnwrappingIndexedListSerializer(_elementType, _staticTyping, _valueTypeSerializer, _elementSerializer, t);
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

    private void substituteElementName(Object elem, JsonGenerator g) {
        if (nextElementSubstitutionMap != null && g instanceof ToXmlGenerator) {
            String substitutedName = nextElementSubstitutionMap.getSubstitutedName(elem);
            ((ToXmlGenerator) g).setNextName(new QName(substitutedName));
        }
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
                    substituteElementName(elem, g);
                    serializer.serialize(elem, g, provider);
                }
            }
            nextElementSubstitutionMap = null;
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
                substituteElementName(elem, jgen);
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
        nextElementSubstitutionMap = null;
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
                    substituteElementName(elem, jgen);
                    serializer.serializeWithType(elem, jgen, provider, typeSer);
                }
            }
            nextElementSubstitutionMap = null;
        } catch (Exception e) {
            wrapAndThrow(provider, e, value, i);
        }
    }
}
