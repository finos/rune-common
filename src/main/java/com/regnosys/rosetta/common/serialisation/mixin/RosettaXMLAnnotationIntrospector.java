package com.regnosys.rosetta.common.serialisation.mixin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.*;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.AttributePropertyWriter;
import com.fasterxml.jackson.databind.util.SimpleBeanPropertyDefinition;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlAnnotationIntrospector;
import com.rosetta.model.lib.ModelSymbolId;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.util.DottedPath;
import com.rosetta.util.serialisation.AttributeXMLConfiguration;
import com.rosetta.util.serialisation.AttributeXMLRepresentation;
import com.rosetta.util.serialisation.RosettaXMLConfiguration;
import com.rosetta.util.serialisation.TypeXMLConfiguration;

import java.util.List;
import java.util.Optional;

public class RosettaXMLAnnotationIntrospector extends JacksonXmlAnnotationIntrospector {

    private static final long serialVersionUID = 1L;
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
    public PropertyName findRootName(AnnotatedClass ac) {
        return getTypeXMLConfiguration(ac)
                .flatMap(TypeXMLConfiguration::getXmlRootElementName)
                .map(PropertyName::construct)
                .orElseGet(() -> super.findRootName(ac));
    }

    @Override
    protected PropertyName _findXmlName(Annotated a) {

        return getAttributeXMLConfiguration(a)
                .flatMap(AttributeXMLConfiguration::getXmlName)
                .map(PropertyName::construct)
                .orElseGet(() -> super._findXmlName(a));

    }






    @Override
    public void findAndAddVirtualProperties(MapperConfig<?> config, AnnotatedClass ac, List<BeanPropertyWriter> properties) {
        getTypeXMLConfiguration(ac)
                .flatMap(TypeXMLConfiguration::getXmlAttributes)
                .ifPresent(xmlAttributes -> {
                    for (final String xmlAttributeName : xmlAttributes.keySet()) {
                        final String xmlAttributeValue = xmlAttributes.get(xmlAttributeName);
                        JavaType propType = config.constructType(Object.class);
                        final BeanPropertyWriter bpw = constructVirtualProperty(xmlAttributeName, xmlAttributeValue, config, ac, propType);
                        properties.add(bpw);
                    }
                });
        super.findAndAddVirtualProperties(config, ac, properties);
    }


    protected BeanPropertyWriter constructVirtualProperty(final String xmlAttributeName, final String xmlAttributeValue, MapperConfig<?> config, AnnotatedClass ac, JavaType type) {
        PropertyName propertyName = _propertyName(xmlAttributeName, "");
        if (!propertyName.hasSimpleName()) {
            propertyName = PropertyName.construct(xmlAttributeName);
        }

        AnnotatedMember member = new VirtualXMLAttribute(ac, ac.getRawType(), xmlAttributeName, type);
        SimpleBeanPropertyDefinition xmlPropertyDefinition = SimpleBeanPropertyDefinition.construct(config, member, propertyName, null, JsonInclude.Include.NON_NULL);
        AttributePropertyWriter apw = new ConstantAttributePropertyWriter(xmlAttributeName, xmlPropertyDefinition, ac.getAnnotations(), type, xmlAttributeValue);
        return apw;
    }

    @Override
    public Boolean isOutputAsAttribute(MapperConfig<?> config, Annotated ann) {
        if (ann instanceof VirtualXMLAttribute) {
            return true;
        }
        return getAttributeXMLConfiguration(ann)
                .flatMap(AttributeXMLConfiguration::getXmlRepresentation)
                .map(attributeXMLRepresentation -> attributeXMLRepresentation == AttributeXMLRepresentation.ATTRIBUTE)
                .orElseGet(() -> super.isOutputAsAttribute(config, ann));
    }

    @Override
    protected boolean _isIgnorable(Annotated a) {
        boolean isIgnorable= super._isIgnorable(a);
        if(isIgnorable){
            return true;
//        } else if (a.hasAnnotation(RosettaAttribute.class) || ((AnnotatedMember) a).getTypeContext()  instanceof  AnnotatedClass && getEnclosingClass((AnnotatedMember) a).getRawType().getSimpleName().startsWith("Measure") && a instanceof AnnotatedField) {
        } else if (a.hasAnnotation(RosettaAttribute.class) ) {
            return false;
        }
        return true;
    }

    @Override
    public Boolean isOutputAsText(MapperConfig<?> config, Annotated ann) {
        return getAttributeXMLConfiguration(ann)
                .flatMap(AttributeXMLConfiguration::getXmlRepresentation)
                .map(attributeXMLRepresentation -> attributeXMLRepresentation == AttributeXMLRepresentation.VALUE)
                .orElseGet(() -> super.isOutputAsText(config, ann));
    }

    @Override
    public String[] findEnumValues(Class<?> enumType, Enum<?>[] enumValues, String[] names) {
        if (rosettaEnumBuilderIntrospector.isApplicable(enumType)) {
            rosettaEnumBuilderIntrospector.findEnumValues(enumType, enumValues, names);
        } else {
            enumAsStringBuilderIntrospector.findEnumValues(enumType, enumValues, names);
        }
        return names;
    }

    @Override
    public void findEnumAliases(Class<?> enumType, Enum<?>[] enumValues, String[][] aliasList) {
        if (rosettaEnumBuilderIntrospector.isApplicable(enumType)) {
            rosettaEnumBuilderIntrospector.findEnumAliases(enumType, enumValues, aliasList);
        } else {
            super.findEnumAliases(enumType, enumValues, aliasList);
        }
    }
    private AnnotatedClass getEnclosingClass(AnnotatedMember member) {
        return (AnnotatedClass) member.getTypeContext(); //TODO: get rid of use of deprecated API
    }

    private Optional<AttributeXMLConfiguration> getAttributeXMLConfiguration(Annotated a) {
        return Optional.of(a)
                .filter(annotated -> annotated instanceof AnnotatedMember)
                .map(annotated -> (AnnotatedMember) annotated)
                .flatMap(annotatedMember ->
                        Optional.ofNullable(annotatedMember.getAnnotation(RosettaAttribute.class))
                                .flatMap(rosettaAttributeAnnotation ->
                                        getTypeXMLConfiguration(getEnclosingClass(annotatedMember))
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
