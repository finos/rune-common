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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.PropertyMetadata;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.*;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.AttributePropertyWriter;
import com.fasterxml.jackson.databind.util.SimpleBeanPropertyDefinition;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlAnnotationIntrospector;
import com.fasterxml.jackson.dataformat.xml.util.TypeUtil;
import com.regnosys.rosetta.common.serialisation.ConstantAttributePropertyWriter;
import com.regnosys.rosetta.common.serialisation.mixin.EnumAsStringBuilderIntrospector;
import com.regnosys.rosetta.common.serialisation.mixin.RosettaEnumBuilderIntrospector;
import com.rosetta.model.lib.ModelSymbolId;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.util.DottedPath;
import com.rosetta.util.serialisation.AttributeXMLConfiguration;
import com.rosetta.util.serialisation.AttributeXMLRepresentation;
import com.rosetta.util.serialisation.RosettaXMLConfiguration;
import com.rosetta.util.serialisation.TypeXMLConfiguration;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class RosettaXMLAnnotationIntrospector extends JacksonXmlAnnotationIntrospector {

    private static final long serialVersionUID = 1L;

    // Our generated code uses 'build' for the build method, 'set' as setter prefix
    private static final JsonPOJOBuilder.Value ROSETTA_BUILDER_CONFIG = new JsonPOJOBuilder.Value("build", "set");

    public static final String SCHEMA_LOCATION_ATTRIBUTE_NAME = "schemaLocation";
    private static final String SCHEMA_LOCATION_ATTRIBUTE_PREFIXED_NAME = "xsi:" + SCHEMA_LOCATION_ATTRIBUTE_NAME;

    private final RosettaXMLConfiguration rosettaXMLConfiguration;

    private final RosettaEnumBuilderIntrospector rosettaEnumBuilderIntrospector;

    private final EnumAsStringBuilderIntrospector enumAsStringBuilderIntrospector;


    public RosettaXMLAnnotationIntrospector(final RosettaXMLConfiguration rosettaXMLConfiguration, final boolean supportNativeEnumValue) {
        this(rosettaXMLConfiguration, new RosettaEnumBuilderIntrospector(supportNativeEnumValue), new EnumAsStringBuilderIntrospector());
    }

    public RosettaXMLAnnotationIntrospector(RosettaXMLConfiguration rosettaXMLConfiguration, RosettaEnumBuilderIntrospector rosettaEnumBuilderIntrospector, final EnumAsStringBuilderIntrospector enumAsStringBuilderIntrospector) {
        this.rosettaXMLConfiguration = rosettaXMLConfiguration;
        this.rosettaEnumBuilderIntrospector = rosettaEnumBuilderIntrospector;
        this.enumAsStringBuilderIntrospector = enumAsStringBuilderIntrospector;
    }

    @Override
    public Class<?> findPOJOBuilder(AnnotatedClass ac) {
        if (ac.hasAnnotation(RosettaDataType.class)) {
            return ac.getAnnotation(RosettaDataType.class).builder();
        }
        return super.findPOJOBuilder(ac);
    }

    @Override
    public JsonPOJOBuilder.Value findPOJOBuilderConfig(AnnotatedClass ac) {
        if (ac.hasAnnotation(RosettaDataType.class)) {
            return ROSETTA_BUILDER_CONFIG;
        }
        return super.findPOJOBuilderConfig(ac);
    }

    @Override
    public PropertyName findRootName(AnnotatedClass ac) {
        // If the root element name is specified in the XML configuration, use that.
        // Otherwise, if the RosettaDataType annotation is present, use the name from that.
        // Otherwise, use the default.
        return getTypeXMLConfiguration(ac)
                .flatMap(TypeXMLConfiguration::getXmlRootElementName)
                .map(PropertyName::construct)
                .orElseGet(() ->
                        Optional.ofNullable(ac.getAnnotation(RosettaDataType.class))
                                .map(rosettaDataTypeAnn -> PropertyName.construct(rosettaDataTypeAnn.value()))
                                .orElseGet(() -> super.findRootName(ac))
                );
    }

    @Override
    protected PropertyName _findXmlName(Annotated a) {
        if (this.shouldUseDefaultPropertyName(a)) {
            // This is an edge case to conform to the same behaviour as the @JacksonXmlText annotation
            // in case where the attribute should be rendered as an XML value.
            return PropertyName.USE_DEFAULT;
        }
        // If the XML name is specified in the XML configuration, use that.
        return this.getAttributeXMLConfiguration(a)
                .flatMap(AttributeXMLConfiguration::getXmlName)
                .map(PropertyName::construct)
                .orElseGet(
                        () -> Optional.ofNullable(a.getAnnotation(RosettaAttribute.class))
                                .map(rosettaAttrAnn -> PropertyName.construct(rosettaAttrAnn.value()))
                                .orElseGet(() -> super._findXmlName(a))
                );
    }

    private boolean shouldUseDefaultPropertyName(Annotated a) {
        return getAttributeXMLConfiguration(a)
                .flatMap(AttributeXMLConfiguration::getXmlRepresentation)
                .map(attributeXMLRepresentation -> attributeXMLRepresentation == AttributeXMLRepresentation.VALUE)
                .orElse(false);
    }

    @Override
    public void findAndAddVirtualProperties(MapperConfig<?> config, AnnotatedClass ac, List<BeanPropertyWriter> properties) {
        getTypeXMLConfiguration(ac)
                .ifPresent(typeXMLConfiguration -> {
                    typeXMLConfiguration.getXmlAttributes().ifPresent(xmlAttributes -> {
                        // For each XML attribute in the configuration, add a virtual XML attribute.
                        for (String xmlAttributeName : xmlAttributes.keySet()) {
                            String xmlAttributeValue = xmlAttributes.get(xmlAttributeName);
                            JavaType propType = config.constructType(String.class);
                            BeanPropertyWriter bpw = constructVirtualXMLAttribute(xmlAttributeName, xmlAttributeValue, config, ac, propType);
                            properties.add(bpw);
                        }
                    });
                    // For the root XML element, add an optional xsi:schemaLocation attribute
                    // that can be configured using `ObjectWriter::withAttribute("schemaLocation", <value>)`.
                    if (typeXMLConfiguration.getXmlRootElementName().isPresent()) {
                        JavaType propType = config.constructType(String.class);
                        BeanPropertyWriter bpw = constructVirtualSchemaLocationAttribute(config, ac, propType);
                        properties.add(bpw);
                    }
                });
        super.findAndAddVirtualProperties(config, ac, properties);
    }

    private BeanPropertyWriter constructVirtualXMLAttribute(final String xmlAttributeName, final String xmlAttributeValue, MapperConfig<?> config, AnnotatedClass ac, JavaType type) {
        PropertyName propertyName = PropertyName.construct(xmlAttributeName);
        AnnotatedMember member = new VirtualXMLAttribute(ac, ac.getRawType(), xmlAttributeName, type);
        SimpleBeanPropertyDefinition xmlPropertyDefinition = SimpleBeanPropertyDefinition.construct(config, member, propertyName, PropertyMetadata.STD_REQUIRED, JsonInclude.Include.NON_NULL);
        return new ConstantAttributePropertyWriter(xmlAttributeName, xmlPropertyDefinition, ac.getAnnotations(), type, xmlAttributeValue);
    }
    private BeanPropertyWriter constructVirtualSchemaLocationAttribute(MapperConfig<?> config, AnnotatedClass ac, JavaType type) {
        PropertyName propertyName = PropertyName.construct(SCHEMA_LOCATION_ATTRIBUTE_PREFIXED_NAME);
        AnnotatedMember member = new VirtualXMLAttribute(ac, ac.getRawType(), SCHEMA_LOCATION_ATTRIBUTE_NAME, type);
        SimpleBeanPropertyDefinition xmlPropertyDefinition = SimpleBeanPropertyDefinition.construct(config, member, propertyName, PropertyMetadata.STD_OPTIONAL, JsonInclude.Include.NON_NULL);
        return AttributePropertyWriter.construct(SCHEMA_LOCATION_ATTRIBUTE_NAME, xmlPropertyDefinition, ac.getAnnotations(), type);
    }

    @Override
    public Boolean isOutputAsAttribute(MapperConfig<?> config, Annotated ann) {
        if (ann instanceof VirtualXMLAttribute) {
            // Edge case: manually constructed `VirtualXMLAttribute` instances should be rendered as attributes.
            return true;
        }
        // If the XML representation for this member equals ATTRIBUTE, render it as an attribute.
        return getAttributeXMLConfiguration(ann)
                .flatMap(AttributeXMLConfiguration::getXmlRepresentation)
                .map(attributeXMLRepresentation -> attributeXMLRepresentation == AttributeXMLRepresentation.ATTRIBUTE)
                .orElseGet(() -> super.isOutputAsAttribute(config, ann));
    }

    @Override
    public Boolean isOutputAsText(MapperConfig<?> config, Annotated ann) {
        // If the XML representation for this member equals VALUE, render it as a value.
        return getAttributeXMLConfiguration(ann)
                .flatMap(AttributeXMLConfiguration::getXmlRepresentation)
                .map(attributeXMLRepresentation -> attributeXMLRepresentation == AttributeXMLRepresentation.VALUE)
                .orElseGet(() -> super.isOutputAsText(config, ann));
    }

    @Override
    protected boolean _isIgnorable(Annotated a) {
        boolean isIgnorable= super._isIgnorable(a);
        if (isIgnorable) {
            return true;
        }
        // Additionally, ignore any members that do not have the RosettaAttribute annotation
        // except for constructors, which are necessary for deserialisation.
        return !(a.hasAnnotation(RosettaAttribute.class) || a instanceof AnnotatedConstructor);
    }

    @Override
    public JsonIgnoreProperties.Value findPropertyIgnoralByName(MapperConfig<?> config, Annotated a) {
        // For the root element, ignore the xsi:schemaLocation attribute.
        JsonIgnoreProperties.Value ignoreProps = super.findPropertyIgnoralByName(config, a);
        return Optional.of(a)
                .filter(ann -> ann instanceof AnnotatedClass)
                .map(ann -> (AnnotatedClass) ann)
                .flatMap(this::getTypeXMLConfiguration)
                .flatMap(TypeXMLConfiguration::getXmlRootElementName)
                .map(rootElementName -> {
                    Set<String> ignoredNames = new HashSet<>(ignoreProps.getIgnored());
                    ignoredNames.add(SCHEMA_LOCATION_ATTRIBUTE_NAME);
                    return JsonIgnoreProperties.Value.construct(
                            ignoredNames,
                            ignoreProps.getIgnoreUnknown(),
                            ignoreProps.getAllowGetters(),
                            ignoreProps.getAllowSetters(),
                            ignoreProps.getMerge());
                }).orElse(ignoreProps);
    }

    @Override
    public PropertyName findWrapperName(Annotated ann) {
        if (ann.hasAnnotation(RosettaAttribute.class) && hasCollectionType(ann)) {
            // Disable wrapping of lists.
            return PropertyName.NO_NAME;
        }
        return super.findWrapperName(ann);
    }

    private boolean hasCollectionType(Annotated ann) {
        if (ann instanceof AnnotatedMethod) {
            AnnotatedMethod method = (AnnotatedMethod) ann;
            JavaType attributeType;
            if (method.getParameterCount() == 1) {
                attributeType = method.getParameterType(0);
            } else {
                attributeType = method.getType();
            }
            return TypeUtil.isIndexedType(attributeType);
        }
        return false;
    }

    @Override
    public String[] findEnumValues(MapperConfig<?> config, AnnotatedClass enumType,
                                   Enum<?>[] enumValues, String[] names) {
        if (rosettaEnumBuilderIntrospector.isApplicable(enumType)) {
            rosettaEnumBuilderIntrospector.findEnumValues(enumType, enumValues, names);
        } else {
            enumAsStringBuilderIntrospector.findEnumValues(enumType, enumValues, names);
        }
        return names;
    }

    @Override
    public void findEnumAliases(MapperConfig<?> config, AnnotatedClass enumType,
                                Enum<?>[] enumValues, String[][] aliasList) {
        if (rosettaEnumBuilderIntrospector.isApplicable(enumType)) {
            rosettaEnumBuilderIntrospector.findEnumAliases(enumType, enumValues, aliasList);
        } else {
            super.findEnumAliases(config, enumType, enumValues, aliasList);
        }
    }

    private AnnotatedClass getEnclosingAnnotatedClass(AnnotatedMember member) {
        // TODO: get rid of use of deprecated API, see issue https://github.com/FasterXML/jackson-databind/issues/4141
        return (AnnotatedClass) member.getTypeContext();
    }

    private Optional<AttributeXMLConfiguration> getAttributeXMLConfiguration(Annotated a) {
        return Optional.of(a)
                .filter(annotated -> annotated instanceof AnnotatedMember)
                .map(annotated -> (AnnotatedMember) annotated)
                .flatMap(annotatedMember ->
                        Optional.ofNullable(annotatedMember.getAnnotation(RosettaAttribute.class))
                                .flatMap(rosettaAttributeAnnotation ->
                                        getTypeXMLConfiguration(getEnclosingAnnotatedClass(annotatedMember))
                                                .flatMap(TypeXMLConfiguration::getAttributes)
                                                .map(attributeMap -> attributeMap.get(rosettaAttributeAnnotation.value())))
                );

    }

    private Optional<TypeXMLConfiguration> getTypeXMLConfiguration(AnnotatedClass ac) {
        return Optional.ofNullable(ac.getAnnotation(RosettaDataType.class)).flatMap(rosettaDataTypeAnnotation -> {
            final String namespace = ac.getType().getRawClass().getPackage().getName();
            final DottedPath dottedPath = DottedPath.splitOnDots(namespace);
            final String name = ac.getAnnotation(RosettaDataType.class).value();
            final ModelSymbolId modelSymbolId = new ModelSymbolId(dottedPath, name);
            return rosettaXMLConfiguration.getConfigurationForType(modelSymbolId);
        });
    }
}
