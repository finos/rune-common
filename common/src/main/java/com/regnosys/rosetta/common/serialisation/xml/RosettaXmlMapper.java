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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;
import com.regnosys.rosetta.common.serialisation.xml.config.RosettaXMLConfiguration;
import com.regnosys.rosetta.common.serialisation.xml.config.TypeXMLConfiguration;
import com.rosetta.model.lib.ModelSymbolId;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * Custom XmlMapper that extends the standard XmlMapper and adds support for top-level
 * XML element substitution.
 */
public class RosettaXmlMapper extends XmlMapper {
    private static final long serialVersionUID = 1L;
    private final RosettaXMLConfiguration config;
    private final ClassLoader classLoader;

    public RosettaXmlMapper(RosettaXMLConfiguration config, ClassLoader classLoader) {
        super((JacksonXmlModule) null);
        this.config = config;
        this.classLoader = classLoader;

    }

    @Override
    protected Object _readValue(DeserializationConfig cfg, JsonParser p,
                                JavaType valueType)
            throws IOException
    {
        JavaType typeFromRootElementName = getTypeFromRootElementName(p);
        Object value;
        if (typeFromRootElementName != null) {
            checkRootTypeIsSubtypeOfProvidedType(typeFromRootElementName, valueType);
            value = super._readValue(cfg, p, typeFromRootElementName);
        } else {
            value = super._readValue(cfg, p, valueType);
        }
        return pruneBuilder(value);
    }

    @Override
    protected Object _readMapAndClose(JsonParser p, JavaType valueType)
            throws IOException
    {
        JavaType typeFromRootElementName = getTypeFromRootElementName(p);
        Object value;
        if (typeFromRootElementName != null) {
            checkRootTypeIsSubtypeOfProvidedType(typeFromRootElementName, valueType);
            value = super._readMapAndClose(p, typeFromRootElementName);
        } else {
            value = super._readMapAndClose(p, valueType);
        }
        return pruneBuilder(value);
    }
    
    private void checkRootTypeIsSubtypeOfProvidedType(JavaType rootType, JavaType providedType) {
        if (rootType.isTypeOrSubTypeOf(providedType.getRawClass())) {
            return;
        }
        throw new IllegalArgumentException("The inferred root type " + rootType + " is not a subtype of the provided type " + providedType + ".");
    }
    
    private JavaType getTypeFromRootElementName(JsonParser p) {
        String rootElementName = getRootXMLElementName(p);
        if (rootElementName == null) {
            return null;
        }
        return getValueTypeForElement(rootElementName)
                .map(this::convertClassToJavaType)
                .orElse(null);
    }
    
    private String getRootXMLElementName(JsonParser p) {
        if (p instanceof FromXmlParser) {
            FromXmlParser fromXmlParser = (FromXmlParser) p;
            return fromXmlParser.getStaxReader().getLocalName();
        }
        return null;
    }

    private Optional<Class<?>> getValueTypeForElement(String elementName) {
        for (Map.Entry<ModelSymbolId, TypeXMLConfiguration> entry : config.getTypeConfigMap().entrySet()) {
            TypeXMLConfiguration typeXMLConfiguration = entry.getValue();
            if (typeXMLConfiguration.getXmlElementName().map(x -> x.equals(elementName)).orElse(false)) {
                ModelSymbolId modelSymbolId = entry.getKey();
                try {
                    Class<?> aClass = loadModelSymbolAsClass(modelSymbolId);
                    return Optional.of(aClass);
                } catch (ClassNotFoundException e) {
                    return Optional.empty();
                }
            }
        }

        return Optional.empty();
    }

    private Class<?> loadModelSymbolAsClass(ModelSymbolId modelSymbolId) throws ClassNotFoundException {
        return classLoader.loadClass(modelSymbolId.getQualifiedName().toString());
    }

    private JavaType convertClassToJavaType(Class<?> clazz) {
        return getTypeFactory().constructType(clazz);
    }

    private Object pruneBuilder(Object value) {
        if (value instanceof RosettaModelObject) {
            return ((RosettaModelObject) value).toBuilder().prune().build();
        }
        return value;
    }
}
