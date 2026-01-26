package com.regnosys.rosetta.common.serialisation.xml.deserialization;

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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.NullValueProvider;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.impl.MethodProperty;
import com.fasterxml.jackson.databind.deser.impl.NullsConstantProvider;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;
import com.regnosys.rosetta.common.serialisation.xml.SubstitutionMap;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * A copy of {@code MethodProperty}, which is a final class,
 * with support for specializing the type to a more specific type.
 *
 * This is necessary for deserialising substitution groups; see
 * {@code RosettaBeanDeserializerModifier}.
 */
public class SubstitutedMethodProperty extends SettableBeanProperty {
    private static final long serialVersionUID = 1;

    protected final AnnotatedMethod _annotated;

    /**
     * Setter method for modifying property value; used for
     * "regular" method-accessible properties.
     */
    protected final transient Method _setter;

    /**
     * @since 2.9
     */
    final protected boolean _skipNulls;

    protected final JavaType _substitutedType;

    protected final SubstitutionMap _substitutionMap;

    public SubstitutedMethodProperty(MethodProperty src, JavaType substitutedType, AnnotatedMethod method, SubstitutionMap substitutionMap) {
        super(src);
        _annotated = method;
        _setter = method.getAnnotated();
        _skipNulls = NullsConstantProvider.isSkipper(_nullProvider);
        _substitutedType = substitutedType;
        _substitutionMap = substitutionMap;
    }

    protected SubstitutedMethodProperty(SubstitutedMethodProperty src, JsonDeserializer<?> deser,
                                        NullValueProvider nva) {
        super(src, deser, nva);
        _annotated = src._annotated;
        _setter = src._setter;
        _skipNulls = NullsConstantProvider.isSkipper(nva);
        _substitutedType = src._substitutedType;
        _substitutionMap = src._substitutionMap;
    }

    protected SubstitutedMethodProperty(SubstitutedMethodProperty src, PropertyName newName) {
        super(src, newName);
        _annotated = src._annotated;
        _setter = src._setter;
        _skipNulls = src._skipNulls;
        _substitutedType = src._substitutedType;
        _substitutionMap = src._substitutionMap;
    }

    /**
     * Constructor used for JDK Serialization when reading persisted object
     */
    protected SubstitutedMethodProperty(SubstitutedMethodProperty src, Method m) {
        super(src);
        _annotated = src._annotated;
        _setter = m;
        _skipNulls = src._skipNulls;
        _substitutedType = src._substitutedType;
        _substitutionMap = src._substitutionMap;
    }

    @Override
    public JavaType getType() {
        return _substitutedType;
    }

    /**
     * Determines the actual type to deserialize based on the namespace information in the XML parser.
     * If the parser is a FromXmlParser and has namespace information, it will look up the correct type
     * from the substitution map. Otherwise, it falls back to the default substituted type.
     */
    private JavaType getActualType(JsonParser p) {
        if (p instanceof FromXmlParser) {
            FromXmlParser xmlParser = (FromXmlParser) p;
            QName staxName = xmlParser.getStaxReader().getName();
            if (staxName != null) {
                String namespaceURI = staxName.getNamespaceURI();
                String localName = staxName.getLocalPart();
                if (namespaceURI != null && !namespaceURI.isEmpty()) {
                    String fullyQualifiedName = namespaceURI + "/" + localName;
                    JavaType typeByFqn = _substitutionMap.getTypeByFullyQualifiedName(fullyQualifiedName);
                    if (typeByFqn != null) {
                        return typeByFqn;
                    }
                }
            }
        }
        return _substitutedType;
    }

    @Override
    public SettableBeanProperty withName(PropertyName newName) {
        return new SubstitutedMethodProperty(this, newName);
    }

    @Override
    public SubstitutedMethodProperty withValueDeserializer(JsonDeserializer<?> deser) {
        if (_valueDeserializer == deser) {
            return this;
        }
        // 07-May-2019, tatu: As per [databind#2303], must keep VD/NVP in-sync if they were
        NullValueProvider nvp = (_valueDeserializer == _nullProvider) ? deser : _nullProvider;
        return new SubstitutedMethodProperty(this, deser, nvp);
    }

    @Override
    public SettableBeanProperty withNullProvider(NullValueProvider nva) {
        return new SubstitutedMethodProperty(this, _valueDeserializer, nva);
    }

    @Override
    public void fixAccess(DeserializationConfig config) {
        _annotated.fixAccess(
                config.isEnabled(MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS));
    }

    /*
    /**********************************************************
    /* BeanProperty impl
    /**********************************************************
     */

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> acls) {
        return (_annotated == null) ? null : _annotated.getAnnotation(acls);
    }

    @Override
    public AnnotatedMember getMember() {
        return _annotated;
    }

    /*
    /**********************************************************
    /* Overridden methods
    /**********************************************************
     */

    @Override
    public void deserializeAndSet(JsonParser p, DeserializationContext ctxt,
                                  Object instance) throws IOException {
        Object value;
        if (p.hasToken(JsonToken.VALUE_NULL)) {
            if (_skipNulls) {
                return;
            }
            value = _nullProvider.getNullValue(ctxt);
        } else if (_valueTypeDeserializer == null) {
            // Check if we need to use a different deserializer based on namespace
            JavaType actualType = getActualType(p);
            JsonDeserializer<?> deserializer = _valueDeserializer;
            if (!actualType.equals(_substitutedType)) {
                // Get the deserializer for the actual type
                deserializer = ctxt.findRootValueDeserializer(actualType);
            }
            value = deserializer.deserialize(p, ctxt);
            // 04-May-2018, tatu: [databind#2023] Coercion from String (mostly) can give null
            if (value == null) {
                if (_skipNulls) {
                    return;
                }
                value = _nullProvider.getNullValue(ctxt);
            }
        } else {
            value = _valueDeserializer.deserializeWithType(p, ctxt, _valueTypeDeserializer);
        }
        try {
            _setter.invoke(instance, value);
        } catch (Exception e) {
            _throwAsIOE(p, e, value);
        }
    }

    @Override
    public Object deserializeSetAndReturn(JsonParser p,
                                          DeserializationContext ctxt, Object instance) throws IOException {
        Object value;
        if (p.hasToken(JsonToken.VALUE_NULL)) {
            if (_skipNulls) {
                return instance;
            }
            value = _nullProvider.getNullValue(ctxt);
        } else if (_valueTypeDeserializer == null) {
            // Check if we need to use a different deserializer based on namespace
            JavaType actualType = getActualType(p);
            JsonDeserializer<?> deserializer = _valueDeserializer;
            if (!actualType.equals(_substitutedType)) {
                // Get the deserializer for the actual type
                deserializer = ctxt.findRootValueDeserializer(actualType);
            }
            value = deserializer.deserialize(p, ctxt);
            // 04-May-2018, tatu: [databind#2023] Coercion from String (mostly) can give null
            if (value == null) {
                if (_skipNulls) {
                    return instance;
                }
                value = _nullProvider.getNullValue(ctxt);
            }
        } else {
            value = _valueDeserializer.deserializeWithType(p, ctxt, _valueTypeDeserializer);
        }
        try {
            Object result = _setter.invoke(instance, value);
            return (result == null) ? instance : result;
        } catch (Exception e) {
            _throwAsIOE(p, e, value);
            return null;
        }
    }

    @Override
    public final void set(Object instance, Object value) throws IOException {
        if (value == null) {
            if (_skipNulls) {
                return;
            }
        }
        try {
            _setter.invoke(instance, value);
        } catch (Exception e) {
            // 15-Sep-2015, tatu: How could we get a ref to JsonParser?
            _throwAsIOE(e, value);
        }
    }

    @Override
    public Object setAndReturn(Object instance, Object value) throws IOException {
        if (value == null) {
            if (_skipNulls) {
                return instance;
            }
        }
        try {
            Object result = _setter.invoke(instance, value);
            return (result == null) ? instance : result;
        } catch (Exception e) {
            // 15-Sep-2015, tatu: How could we get a ref to JsonParser?
            _throwAsIOE(e, value);
            return null;
        }
    }

    /*
    /**********************************************************
    /* JDK serialization handling
    /**********************************************************
     */

    Object readResolve() {
        return new SubstitutedMethodProperty(this, _annotated.getAnnotated());
    }
}
