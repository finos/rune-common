package com.regnosys.rosetta.common.serialisation.xml.serialization;

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
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContainerSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.impl.PropertySerializerMap;
import com.fasterxml.jackson.databind.util.NameTransformer;

import java.io.IOException;

/**
 * A copy of the class {@code AsArraySerializerBase},
 * except that it will unwrap all items.
 */
public abstract class UnwrappingAsArraySerializerBase<T>
        extends ContainerSerializer<T>
        implements ContextualSerializer {
    protected final JavaType _elementType;

    /**
     * Collection-valued property being serialized with this instance
     */
    protected final BeanProperty _property;

    protected final boolean _staticTyping;

    /**
     * Type serializer used for values, if any.
     */
    protected final TypeSerializer _valueTypeSerializer;

    /**
     * Value serializer to use, if it can be statically determined
     */
    protected final JsonSerializer<Object> _elementSerializer;

    /**
     * If element type cannot be statically determined, mapping from
     * runtime type to serializer is handled using this object
     */
    protected PropertySerializerMap _dynamicSerializers;

    /**
     * Transformer used to add prefix and/or suffix for properties
     * of unwrapped POJO.
     */
    protected final NameTransformer _nameTransformer;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    /**
     * Non-contextual, "blueprint" constructor typically called when the first
     * instance is created, without knowledge of property it was used via.
     *
     * @since 2.6
     */
    protected UnwrappingAsArraySerializerBase(Class<?> cls, JavaType et, boolean staticTyping,
                                              TypeSerializer vts, JsonSerializer<Object> elementSerializer, NameTransformer nameTransformer) {
        this(cls, et, staticTyping, vts, null, elementSerializer, nameTransformer);
    }

    /**
     * General purpose constructor. Use contextual constructors, if possible.
     *
     * @since 2.12
     */
    @SuppressWarnings("unchecked")
    protected UnwrappingAsArraySerializerBase(Class<?> cls, JavaType elementType, boolean staticTyping,
                                              TypeSerializer vts, BeanProperty property, JsonSerializer<?> elementSerializer, NameTransformer nameTransformer) {
        // typing with generics is messy... have to resort to this:
        super(cls, false);
        _elementType = elementType;
        // static if explicitly requested, or if element type is final
        _staticTyping = staticTyping || (elementType != null && elementType.isFinal());
        _valueTypeSerializer = vts;
        _property = property;
        _elementSerializer = (JsonSerializer<Object>) elementSerializer;
        _dynamicSerializers = PropertySerializerMap.emptyForProperties();
        _nameTransformer = nameTransformer;
    }

    @SuppressWarnings("unchecked")
    protected UnwrappingAsArraySerializerBase(UnwrappingAsArraySerializerBase<?> src,
                                              BeanProperty property, TypeSerializer vts, JsonSerializer<?> elementSerializer) {
        super(src);
        _elementType = src._elementType;
        _staticTyping = src._staticTyping;
        _nameTransformer = src._nameTransformer;
        _valueTypeSerializer = vts;
        _property = property;
        _elementSerializer = (JsonSerializer<Object>) elementSerializer;
        // [databind#2181]: may not be safe to reuse, start from empty
        _dynamicSerializers = PropertySerializerMap.emptyForProperties();
    }

    /**
     * @since 2.6
     */
    public abstract UnwrappingAsArraySerializerBase<T> withResolved(BeanProperty property,
                                                                    TypeSerializer vts, JsonSerializer<?> elementSerializer);

    /*
    /**********************************************************
    /* Post-processing
    /**********************************************************
     */

    /**
     * This method is needed to resolve contextual annotations like
     * per-property overrides, as well as do recursive call
     * to <code>createContextual</code> of content serializer, if
     * known statically.
     */
    @Override
    public JsonSerializer<?> createContextual(SerializerProvider serializers,
                                              BeanProperty property)
            throws JsonMappingException {
        TypeSerializer typeSer = _valueTypeSerializer;
        if (typeSer != null) {
            typeSer = typeSer.forProperty(property);
        }
        JsonSerializer<?> ser = null;
        // First: if we have a property, may have property-annotation overrides

        if (property != null) {
            final AnnotationIntrospector intr = serializers.getAnnotationIntrospector();
            AnnotatedMember m = property.getMember();
            if (m != null) {
                Object serDef = intr.findContentSerializer(m);
                if (serDef != null) {
                    ser = serializers.serializerInstance(m, serDef);
                }
            }
        }
        if (ser == null) {
            ser = _elementSerializer;
        }
        // 18-Feb-2013, tatu: May have a content converter:
        ser = findContextualConvertingSerializer(serializers, property, ser);
        if (ser == null) {
            // 30-Sep-2012, tatu: One more thing -- if explicit content type is annotated,
            //   we can consider it a static case as well.
            if (_elementType != null) {
                if (_staticTyping && !_elementType.isJavaLangObject()) {
                    ser = serializers.findContentValueSerializer(_elementType, property);
                }
            }
        }
        if ((ser != _elementSerializer)
                || (property != _property)
                || (_valueTypeSerializer != typeSer)) {
            return withResolved(property, typeSer, ser);
        }
        return this;
    }

    /*
    /**********************************************************
    /* Accessors
    /**********************************************************
     */

    @Override
    public JavaType getContentType() {
        return _elementType;
    }

    @Override
    public JsonSerializer<?> getContentSerializer() {
        return _elementSerializer;
    }

    /*
    /**********************************************************
    /* Serialization
    /**********************************************************
     */

    // NOTE: as of 2.5, sub-classes SHOULD override (in 2.4 and before, was final),
    // at least if they can provide access to actual size of value and use `writeStartArray()`
    // variant that passes size of array to output, which is helpful with some data formats
    @Override
    public void serialize(T value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        serializeContents(value, gen, provider);
    }

    @Override
    public void serializeWithType(T value, JsonGenerator gen, SerializerProvider provider,
                                  TypeSerializer typeSer) throws IOException {
        if (provider.isEnabled(SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS)) {
            provider.reportBadDefinition(handledType(),
                    "Unwrapped property requires use of type information: cannot serialize without disabling `SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS`");
        }
        gen.assignCurrentValue(value); // [databind#631]
        serializeContents(value, gen, provider);
    }

    protected abstract void serializeContents(T value, JsonGenerator gen, SerializerProvider provider)
            throws IOException;

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
            throws JsonMappingException
    {}

    protected final JsonSerializer<Object> _findAndAddDynamic(PropertySerializerMap map,
                                                              Class<?> type, SerializerProvider provider) throws JsonMappingException {
        JsonSerializer<Object> serializer = provider.findContentValueSerializer(type, _property);
        serializer = serializer.unwrappingSerializer(_nameTransformer);

        _dynamicSerializers = _dynamicSerializers.newWith(type, serializer);
        return serializer;
    }

    protected final JsonSerializer<Object> _findAndAddDynamic(PropertySerializerMap map,
                                                              JavaType type, SerializerProvider provider) throws JsonMappingException {
        JsonSerializer<Object> serializer = provider.findContentValueSerializer(type, _property);
        serializer = serializer.unwrappingSerializer(_nameTransformer);

        _dynamicSerializers = _dynamicSerializers.newWith(type.getRawClass(), serializer);
        return serializer;
    }
}
