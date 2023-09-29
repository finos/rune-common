package com.regnosys.rosetta.common.serialisation.mixin;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.annotation.JsonAppend;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.*;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.AttributePropertyWriter;
import com.fasterxml.jackson.databind.util.SimpleBeanPropertyDefinition;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlAnnotationIntrospector;
import com.fasterxml.jackson.dataformat.xml.ser.XmlBeanSerializerBase;
import com.fasterxml.jackson.dataformat.xml.util.XmlInfo;
import com.rosetta.model.lib.ModelSymbolId;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.util.DottedPath;
import com.rosetta.util.serialisation.AttributeXMLRepresentation;
import com.rosetta.util.serialisation.RosettaXMLConfiguration;
import com.rosetta.util.serialisation.TypeXMLConfiguration;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
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
    public PropertyName findWrapperName(Annotated ann) {
        return super.findWrapperName(ann);
    }

    @Override
    public PropertyName findRootName(AnnotatedClass ac) {
        if (ac.hasAnnotation(RosettaDataType.class)) {
            final String namespace = ac.getType().getRawClass().getPackage().getName();
            final DottedPath dottedPath = DottedPath.splitOnDots(namespace);
            final String name = ac.getAnnotation(RosettaDataType.class).value();
            final ModelSymbolId modelSymbolId = new ModelSymbolId(dottedPath, name);
            final Optional<TypeXMLConfiguration> typeXMLConfiguration = rosettaXMLConfiguration.getConfigurationForType(modelSymbolId);
            if (typeXMLConfiguration.isPresent()) {
                if (typeXMLConfiguration.get().getXmlRootElementName().isPresent()) {
                    final String rootName = typeXMLConfiguration.get().getXmlRootElementName().get();
                    return new PropertyName(rootName);
                }
            }
        }
        return super.findRootName(ac);
    }

    @Override
    public PropertyName findNameForSerialization(Annotated a) {
//        if (a.hasAnnotation(RosettaAttribute.class)) {
//            final String namespace = a.getType().getRawClass().getPackage().getName();
//            final DottedPath dottedPath = DottedPath.splitOnDots(namespace);
//            String className = "";
//            if (a instanceof AnnotatedField) {
//                className = ((AnnotatedField) a).getDeclaringClass().getInterfaces()[0].getAnnotation(RosettaDataType.class).value(); //TODO: cleaner solution!
//            } else {
//                return super.findNameForSerialization(a);
//
//            }
//            final ModelSymbolId modelSymbolId = new ModelSymbolId(dottedPath, className);
//            final Optional<TypeXMLConfiguration> typeXMLConfiguration = rosettaXMLConfiguration.getConfigurationForType(modelSymbolId);
//            if (typeXMLConfiguration.isPresent()) {
//                if (typeXMLConfiguration.get().getAttributes().isPresent()) {
//                    final String attributeName = a.getAnnotation(RosettaAttribute.class).value();
//                    final Optional<String> xmlName = typeXMLConfiguration.get().getAttributes().get().get(attributeName).getXmlName();
//                    if (xmlName.isPresent()) {
//                        return new PropertyName(xmlName.get());
//                    }
//                }
//            }
//
//        }
        return super.findNameForSerialization(a);
    }

    @Override
    protected PropertyName _findXmlName(Annotated a) {
        if (a.hasAnnotation(RosettaAttribute.class)) {
            String namespace = a.getType().getRawClass().getPackage().getName();
            String className = "";
            if (a instanceof AnnotatedMember) {
                namespace = ((AnnotatedMethod) a).getDeclaringClass().getPackage().getName();
                className = ((AnnotatedMethod) a).getDeclaringClass().getInterfaces()[0].getAnnotation(RosettaDataType.class).value(); //TODO: cleaner solution!
            }
            final DottedPath dottedPath = DottedPath.splitOnDots(namespace);
            final ModelSymbolId modelSymbolId = new ModelSymbolId(dottedPath, className);
            final Optional<TypeXMLConfiguration> typeXMLConfiguration = rosettaXMLConfiguration.getConfigurationForType(modelSymbolId);
            if (typeXMLConfiguration.isPresent()) {
                if (typeXMLConfiguration.get().getAttributes().isPresent()) {
                    final String attributeName = a.getAnnotation(RosettaAttribute.class).value();
                    final Optional<String> xmlName = typeXMLConfiguration.get().getAttributes().get().get(attributeName).getXmlName();
                    if (xmlName.isPresent()) {
                        return new PropertyName(xmlName.get());
                    }
                }
            }
        }

        return super._findXmlName(a);
    }

//    @Override
//    public String findNamespace(MapperConfig<?> config, Annotated ann) {
////        final String namespace0 = config.findRootName(RosettaAttribute.class).getNamespace();
//        if (ann.hasAnnotation(RosettaAttribute.class)) {
//            final String namespace = ann.getType().getRawClass().getPackage().getName();
//            final DottedPath dottedPath = DottedPath.splitOnDots(namespace);
//            String className = "";
//            if (ann instanceof AnnotatedMember) {
//                className = ((AnnotatedMethod) ann).getDeclaringClass().getInterfaces()[0].getAnnotation(RosettaDataType.class).value(); //TODO: cleaner solution!
//            }
//            final ModelSymbolId modelSymbolId = new ModelSymbolId(dottedPath, className);
//            final Optional<TypeXMLConfiguration> typeXMLConfiguration = rosettaXMLConfiguration.getConfigurationForType(modelSymbolId);
//            if (typeXMLConfiguration.isPresent()) {
//                if (typeXMLConfiguration.get().getXmlAttributes().isPresent()) {
//                    final String attributeName = ann.getAnnotation(RosettaAttribute.class).value();
//                    final Collection<String> xmlAttributes = Collections.singleton(typeXMLConfiguration.get().getXmlAttributes().get().toString());
//                    return "testXMLValue";
//                }
//            }
//        }
//        return super.findNamespace(config, ann);
//    }


    @Override
    public void findAndAddVirtualProperties(MapperConfig<?> config, AnnotatedClass ac, List<BeanPropertyWriter> properties) {
        if (ac.hasAnnotation(RosettaDataType.class)) {
            final String namespace = ac.getType().getRawClass().getPackage().getName();
            final DottedPath dottedPath = DottedPath.splitOnDots(namespace);
            final String name = ac.getAnnotation(RosettaDataType.class).value();
            final ModelSymbolId modelSymbolId = new ModelSymbolId(dottedPath, name);
            final Optional<TypeXMLConfiguration> typeXMLConfiguration = rosettaXMLConfiguration.getConfigurationForType(modelSymbolId);
            if (typeXMLConfiguration.isPresent()) {
                if (typeXMLConfiguration.get().getXmlAttributes().isPresent()) {
                    final Map<String, String> xmlAttributes = typeXMLConfiguration.get().getXmlAttributes().get();
                    for (final String xmlAttributeName: xmlAttributes.keySet()) {
                        final String xmlAttributeValue = xmlAttributes.get(xmlAttributeName);
                        JavaType propType = config.constructType(Object.class);
                        final BeanPropertyWriter bpw = constructVirtualProperty(xmlAttributeName, xmlAttributeValue, config, ac, propType);
                        properties.add(bpw);
                    }
                }
            }
        }
        super.findAndAddVirtualProperties(config, ac, properties);
    }


    protected BeanPropertyWriter constructVirtualProperty(final String xmlAttributeName, final String xmlAttributeValue,  MapperConfig<?> config, AnnotatedClass ac, JavaType type) {
        PropertyName propertyName = _propertyName(xmlAttributeName,"");
        if (!propertyName.hasSimpleName()){
            propertyName = PropertyName.construct(xmlAttributeName);
        }

        AnnotatedMember member = new VirtualXMLAttribute(ac, ac.getRawType(), xmlAttributeName, type);
        SimpleBeanPropertyDefinition xmlPropertyDefinition = SimpleBeanPropertyDefinition.construct(config, member, propertyName,null, JsonInclude.Include.NON_NULL);
        AttributePropertyWriter apw = new ConstantAttributePropertyWriter(xmlAttributeName, xmlPropertyDefinition, ac.getAnnotations(), type, xmlAttributeValue);
        return apw;
    }

    @Override
    public PropertyName findNameForDeserialization(Annotated a) {
        return super.findNameForDeserialization(a);
    }

    @Override
    public Boolean isOutputAsAttribute(MapperConfig<?> config, Annotated ann) {
        if(ann instanceof VirtualXMLAttribute ) {
            return true;
        }
        if (ann.hasAnnotation(RosettaAttribute.class)) {
            String namespace = ann.getType().getRawClass().getPackage().getName();
            String className = "";
            if (ann instanceof AnnotatedMember) {
                namespace = ((AnnotatedMethod) ann).getDeclaringClass().getPackage().getName();
                className = ((AnnotatedMethod) ann).getDeclaringClass().getInterfaces()[0].getAnnotation(RosettaDataType.class).value(); //TODO: cleaner solution!
            }
            final DottedPath dottedPath = DottedPath.splitOnDots(namespace);
            final ModelSymbolId modelSymbolId = new ModelSymbolId(dottedPath, className);
            final Optional<TypeXMLConfiguration> typeXMLConfiguration = rosettaXMLConfiguration.getConfigurationForType(modelSymbolId);
            if (typeXMLConfiguration.isPresent()) {
                if (typeXMLConfiguration.get().getAttributes().isPresent()) {
                    final String attributeName = ann.getAnnotation(RosettaAttribute.class).value();
                    AttributeXMLRepresentation xmlRepresentation = null;
                    if(typeXMLConfiguration.get().getAttributes().get().get(attributeName).getXmlRepresentation().isPresent()){
                         xmlRepresentation = typeXMLConfiguration.get().getAttributes().get().get(attributeName).getXmlRepresentation().get();
                    } else {
                        return false;
                    }
                    return xmlRepresentation == AttributeXMLRepresentation.ATTRIBUTE;
                }
            }
        }
        return super.isOutputAsAttribute(config, ann);
    }

    @Override
    public Boolean isOutputAsText(MapperConfig<?> config, Annotated ann) {
        if (ann.hasAnnotation(RosettaAttribute.class)) {
            String namespace = ann.getType().getRawClass().getPackage().getName();
            String className = "";
            if (ann instanceof AnnotatedMember) {
                namespace = ((AnnotatedMethod) ann).getDeclaringClass().getPackage().getName();
                className = ((AnnotatedMethod) ann).getDeclaringClass().getInterfaces()[0].getAnnotation(RosettaDataType.class).value(); //TODO: cleaner solution!
            }
            final DottedPath dottedPath = DottedPath.splitOnDots(namespace);
            final ModelSymbolId modelSymbolId = new ModelSymbolId(dottedPath, className);
            final Optional<TypeXMLConfiguration> typeXMLConfiguration = rosettaXMLConfiguration.getConfigurationForType(modelSymbolId);
            if (typeXMLConfiguration.isPresent()) {
                if (typeXMLConfiguration.get().getAttributes().isPresent()) {
                    final String attributeName = ann.getAnnotation(RosettaAttribute.class).value();
                    AttributeXMLRepresentation xmlRepresentation = null;
                    if(typeXMLConfiguration.get().getAttributes().get().get(attributeName).getXmlRepresentation().isPresent()){
                        xmlRepresentation = typeXMLConfiguration.get().getAttributes().get().get(attributeName).getXmlRepresentation().get();
                    } else {
                        return false;
                    }
                    return xmlRepresentation == AttributeXMLRepresentation.VALUE;
                }
            }
        }
        return super.isOutputAsText(config, ann);
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
}
