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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyMetadata;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.*;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.util.NameTransformer;
import com.fasterxml.jackson.databind.util.SimpleBeanPropertyDefinition;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlAnnotationIntrospector;
import com.fasterxml.jackson.dataformat.xml.util.TypeUtil;
import com.google.common.collect.Streams;
import com.regnosys.rosetta.common.serialisation.ConstantAttributePropertyWriter;
import com.regnosys.rosetta.common.serialisation.mixin.EnumAsStringBuilderIntrospector;
import com.regnosys.rosetta.common.serialisation.mixin.RosettaEnumBuilderIntrospector;
import com.rosetta.model.lib.ModelSymbolId;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;
import com.rosetta.util.DottedPath;
import com.rosetta.util.serialisation.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RosettaXMLAnnotationIntrospector extends JacksonXmlAnnotationIntrospector {

    private static final long serialVersionUID = 1L;

    // Our generated code uses 'build' for the build method, 'set' as setter prefix
    private static final JsonPOJOBuilder.Value ROSETTA_BUILDER_CONFIG = new JsonPOJOBuilder.Value("build", "set");

    private final RosettaXMLConfiguration rosettaXMLConfiguration;

    private final RosettaEnumBuilderIntrospector rosettaEnumBuilderIntrospector;

    private final EnumAsStringBuilderIntrospector enumAsStringBuilderIntrospector;

    // Note: this is a hack! In some occasions, the methods below
    // do not have access to the `MapperConfig<?>`, but they need it,
    // so having access to the object mapper makes sure we can always
    // access it.
    // Related to https://github.com/FasterXML/jackson-databind/issues/4141.
    private final ObjectMapper mapper;


    public RosettaXMLAnnotationIntrospector(ObjectMapper mapper, final RosettaXMLConfiguration rosettaXMLConfiguration, final boolean supportNativeEnumValue) {
        this(mapper, rosettaXMLConfiguration, new RosettaEnumBuilderIntrospector(supportNativeEnumValue), new EnumAsStringBuilderIntrospector());
    }

    public RosettaXMLAnnotationIntrospector(ObjectMapper mapper, RosettaXMLConfiguration rosettaXMLConfiguration, RosettaEnumBuilderIntrospector rosettaEnumBuilderIntrospector, final EnumAsStringBuilderIntrospector enumAsStringBuilderIntrospector) {
        this.mapper = mapper;
        this.rosettaXMLConfiguration = rosettaXMLConfiguration;
        this.rosettaEnumBuilderIntrospector = rosettaEnumBuilderIntrospector;
        this.enumAsStringBuilderIntrospector = enumAsStringBuilderIntrospector;
    }

    /*

        1. Want to parse an attribute called commoditySwapLeg (ie. in the CommoditySwapDetailsModel)
        2. If the attribute has an elementRef lookup the xml element object in the elements array of the types by fully qualified name
        3. For each element, (ie physicalLeg) lookup all elements that have a substitution group equal to that element name so, so substitution group = physicalLeg
            - For each element found:
                - if is not abstract store link between name of element and type of element in the substitutionMap and recurse
                - if is abstract carry on recursing but don't store in substitutionMap
     */
    public SubstitutionMap  findSubstitutionMap(MapperConfig<?> config, AnnotatedMember member, ClassLoader classLoader) {
        AnnotatedClass ac = getAnnotatedClassOrContent(config, member);
        RosettaDataType ann = ac.getAnnotation(RosettaDataType.class);

        if (ann != null) {
            Map<JavaType, String> substitutionMap = new HashMap<>();
            getAttributeXMLConfiguration(config, member)
                    .flatMap(this::getElementRef)
                    .ifPresent(elementRef -> {
                        lookupElementByFullyQualifiedName(config, elementRef, substitutionMap, classLoader);
                        lookupTransitiveSubstitutionGroups(config, elementRef, substitutionMap, classLoader);
                    });
            lookupLegacySubstitutionsForType(config, ac, ann, substitutionMap, classLoader);
            if (substitutionMap.isEmpty()) {
                return null;
            }
            return new SubstitutionMap(substitutionMap);
        }
        return null;
    }

    /*
     * Required for backwards compatibility getElementRef() supersedes legacy getSubstitutionGroup() method
     */
    private Optional<String> getElementRef(AttributeXMLConfiguration attributeXMLConfiguration) {
        return attributeXMLConfiguration.getElementRef().isPresent() ? attributeXMLConfiguration.getElementRef() : attributeXMLConfiguration.getSubstitutionGroup();
    }

    private void lookupElementByFullyQualifiedName(MapperConfig<?> config, String fullyQualifiedName, Map<JavaType, String> substitutionMap, ClassLoader classLoader) {
        SortedMap<ModelSymbolId, TypeXMLConfiguration> typeConfigMap = rosettaXMLConfiguration.getTypeConfigMap();
        for (Map.Entry<ModelSymbolId, TypeXMLConfiguration> modelSymbolIdTypeXMLConfigurationEntry : typeConfigMap.entrySet()) {
            TypeXMLConfiguration typeXMLConfiguration = modelSymbolIdTypeXMLConfigurationEntry.getValue();
            if (typeXMLConfiguration.getXmlElementFullyQualifiedName().map(x -> x.equals(fullyQualifiedName)).orElse(false)) {
                updateSubstitutionMap(config, substitutionMap, classLoader, modelSymbolIdTypeXMLConfigurationEntry, typeXMLConfiguration);

            }
        }
    }

    private void lookupTransitiveSubstitutionGroups(MapperConfig<?> config, String substitutionGroup, Map<JavaType, String> substitutionMap, ClassLoader classLoader) {
        SortedMap<ModelSymbolId, TypeXMLConfiguration> typeConfigMap = rosettaXMLConfiguration.getTypeConfigMap();
        for (Map.Entry<ModelSymbolId, TypeXMLConfiguration> modelSymbolIdTypeXMLConfigurationEntry : typeConfigMap.entrySet()) {
            TypeXMLConfiguration typeXMLConfiguration = modelSymbolIdTypeXMLConfigurationEntry.getValue();
            if (typeXMLConfiguration.getSubstitutionGroup().map(g -> g.equals(substitutionGroup)).orElse(false)) {
                updateSubstitutionMap(config, substitutionMap, classLoader, modelSymbolIdTypeXMLConfigurationEntry, typeXMLConfiguration);
                typeXMLConfiguration.getXmlElementFullyQualifiedName().ifPresent(fullyQualifiedName -> lookupTransitiveSubstitutionGroups(config, fullyQualifiedName, substitutionMap, classLoader));
            }
        }
    }

    private void updateSubstitutionMap(MapperConfig<?> config, Map<JavaType, String> substitutionMap, ClassLoader classLoader, Map.Entry<ModelSymbolId, TypeXMLConfiguration> modelSymbolIdTypeXMLConfigurationEntry, TypeXMLConfiguration typeXMLConfiguration) {
        if (!typeXMLConfiguration.getAbstract().orElse(false)) {
            ModelSymbolId modelSymbolId = modelSymbolIdTypeXMLConfigurationEntry.getKey();
            try {
                JavaType javaType = config.constructType(classLoader.loadClass(modelSymbolId.toString()));
                typeXMLConfiguration.getXmlElementName().ifPresent(name -> substitutionMap.put(javaType, name));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /*
     * Required for backwards compatibility
     */
    private void lookupLegacySubstitutionsForType(MapperConfig<?> config, AnnotatedClass ac, RosettaDataType ann, Map<JavaType, String> substitutionMap, ClassLoader classLoader) {
        ModelSymbolId id = createModelSymbolId(ac, ann.value());
        List<ModelSymbolId> substitutions = new ArrayList<>(rosettaXMLConfiguration.getSubstitutionsForType(id)); // Old substitution group model field

        if (!substitutions.isEmpty()) {
            substitutionMap.putAll(Streams.concat(substitutions.stream(), Stream.of(id))
                    .collect(Collectors.toMap(
                            s -> {
                                try {
                                    return config.constructType(classLoader.loadClass(s.toString()));
                                } catch (ClassNotFoundException e) {
                                    throw new RuntimeException(e);
                                }
                            },
                            this::getElementName
                    )));
        }
    }

    private String getElementName(ModelSymbolId type) {
        return rosettaXMLConfiguration.getConfigurationForType(type).flatMap(TypeXMLConfiguration::getXmlElementName).orElse(type.getName());
    }

    @Override
    public NameTransformer findUnwrappingNameTransformer(AnnotatedMember member) {
        return getAttributeXMLConfiguration(mapper.getDeserializationConfig(), member)
                .flatMap(AttributeXMLConfiguration::getXmlRepresentation)
                .filter(attributeXMLRepresentation -> attributeXMLRepresentation == AttributeXMLRepresentation.VIRTUAL)
                .map(repr -> NameTransformer.NOP)
                .orElseGet(() -> super.findUnwrappingNameTransformer(member));
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
        // If the element name is specified in the XML configuration, use that.
        // Otherwise, if the RosettaDataType annotation is present, use the name from that.
        // Otherwise, use the default.
        return getTypeXMLConfigurations(mapper.getSerializationConfig(), ac).stream()
                .filter(t -> t.getXmlElementName().isPresent())
                .map(t -> t.getXmlElementName().get())
                .findFirst()
                .map(PropertyName::construct)
                .orElseGet(() ->
                        Optional.ofNullable(super.findRootName(ac))
                                .orElseGet(() -> Optional.ofNullable(ac.getAnnotation(RosettaDataType.class))
                                        .map(rosettaDataTypeAnn -> PropertyName.construct(rosettaDataTypeAnn.value()))
                                        .orElse(null))
                );
    }

    @Override
    protected PropertyName _findXmlName(Annotated a) {
        if (a.getRawType().equals(List.class) && getAttributeXMLConfiguration(mapper.getSerializationConfig(), a).flatMap(AttributeXMLConfiguration::getXmlRepresentation).map(repr -> repr == AttributeXMLRepresentation.VIRTUAL).orElse(false)) {
            return PropertyName.NO_NAME;
        }
        if (this.shouldUseDefaultPropertyName(mapper.getSerializationConfig(), a)) {
            // This is an edge case to conform to the same behaviour as the @JacksonXmlText annotation
            // in case where the attribute should be rendered as an XML value.
            return PropertyName.USE_DEFAULT;
        }
        // If the XML name is specified in the XML configuration, use that.
        return this.getAttributeXMLConfiguration(mapper.getSerializationConfig(), a)
                .flatMap(AttributeXMLConfiguration::getXmlName)
                .map(PropertyName::construct)
                .orElseGet(
                        () -> Optional.ofNullable(a.getAnnotation(RosettaAttribute.class))
                                .map(rosettaAttrAnn -> PropertyName.construct(rosettaAttrAnn.value()))
                                .orElseGet(() -> super._findXmlName(a))
                );
    }

    private boolean shouldUseDefaultPropertyName(MapperConfig<?> config, Annotated a) {
        return getAttributeXMLConfiguration(config, a)
                .flatMap(AttributeXMLConfiguration::getXmlRepresentation)
                .map(attributeXMLRepresentation -> attributeXMLRepresentation == AttributeXMLRepresentation.VALUE)
                .orElse(false);
    }

    @Override
    public void findAndAddVirtualProperties(MapperConfig<?> config, AnnotatedClass ac, List<BeanPropertyWriter> properties) {
        getTypeXMLConfigurations(config, ac)
                .forEach(typeXMLConfiguration -> {
                    typeXMLConfiguration.getXmlAttributes().ifPresent(xmlAttributes -> {
                        // For each XML attribute in the configuration, add a virtual XML attribute.
                        for (String xmlAttributeName : xmlAttributes.keySet()) {
                            String xmlAttributeValue = xmlAttributes.get(xmlAttributeName);
                            JavaType propType = config.constructType(String.class);
                            BeanPropertyWriter bpw = constructVirtualXMLAttribute(xmlAttributeName, xmlAttributeValue, config, ac, propType);
                            properties.add(bpw);
                        }
                    });
                });
        super.findAndAddVirtualProperties(config, ac, properties);
    }

    private BeanPropertyWriter constructVirtualXMLAttribute(final String xmlAttributeName, final String xmlAttributeValue, MapperConfig<?> config, AnnotatedClass ac, JavaType type) {
        PropertyName propertyName = PropertyName.construct(xmlAttributeName);
        AnnotatedMember member = new VirtualXMLAttribute(ac.getRawType(), xmlAttributeName, type);
        SimpleBeanPropertyDefinition xmlPropertyDefinition = SimpleBeanPropertyDefinition.construct(config, member, propertyName, PropertyMetadata.STD_REQUIRED, JsonInclude.Include.NON_NULL);
        return new ConstantAttributePropertyWriter(xmlAttributeName, xmlPropertyDefinition, ac.getAnnotations(), type, xmlAttributeValue);
    }

    @Override
    public Boolean isOutputAsAttribute(MapperConfig<?> config, Annotated ann) {
        if (ann instanceof VirtualXMLAttribute) {
            // Edge case: manually constructed `VirtualXMLAttribute` instances should be rendered as attributes.
            return true;
        }
        // If the XML representation for this member equals ATTRIBUTE, render it as an attribute.
        return getAttributeXMLConfiguration(config, ann)
                .flatMap(AttributeXMLConfiguration::getXmlRepresentation)
                .map(attributeXMLRepresentation -> attributeXMLRepresentation == AttributeXMLRepresentation.ATTRIBUTE)
                .orElseGet(() -> super.isOutputAsAttribute(config, ann));
    }

    @Override
    public Boolean isOutputAsText(MapperConfig<?> config, Annotated ann) {
        // If the XML representation for this member equals VALUE, render it as a value.
        return getAttributeXMLConfiguration(config, ann)
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
                // TODO: substitution groups should not have this
                .flatMap(ac -> this.getTypeXMLConfigurations(config, ac).stream().filter(t -> t.getXmlElementName().isPresent()).map(t -> t.getXmlElementName().get()).findFirst())
                .map(rootElementName -> {
                    Set<String> ignoredNames = new HashSet<>(ignoreProps.getIgnored());
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
        getEnumXMLConfigurations(config, enumType).flatMap(TypeXMLConfiguration::getEnumValues).ifPresent(enumMap -> {
            for (int i = 0; i < enumValues.length; i++) {
                Enum<?> value = enumValues[i];
                Field f;
                try {
                    f = value.getDeclaringClass().getField(value.name());
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException(e);
                }
                RosettaEnumValue ann = f.getAnnotation(RosettaEnumValue.class);
                if (ann != null) {
                    String nameOverride = enumMap.get(ann.value());
                    if (nameOverride != null) {
                        names[i] = nameOverride;
                    }
                }
            }
        });
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

    private AnnotatedClass getEnclosingAnnotatedClass(MapperConfig<?> config, AnnotatedMember member) {
        // TODO: see issue https://github.com/FasterXML/jackson-databind/issues/4141
        return AnnotatedClassResolver.resolve(config, config.constructType(member.getDeclaringClass()), config);
    }
    private AnnotatedClass getAnnotatedClassOrContent(MapperConfig<?> config, AnnotatedMember m) {
        JavaType t;
        if (m instanceof AnnotatedMethod) {
            AnnotatedMethod method = (AnnotatedMethod) m;
            if (method.getParameterCount() == 1) {
                // For setters
                t = method.getParameterType(0);
            } else {
                t = method.getType();
            }
        } else {
            t = m.getType();
        }
        if (t.getContentType() != null) {
            t = t.getContentType();
        }
        return AnnotatedClassResolver.resolve(config, t, config);
    }

    private Optional<AttributeXMLConfiguration> getAttributeXMLConfiguration(MapperConfig<?> config, Annotated a) {
        return Optional.of(a)
                .filter(annotated -> annotated instanceof AnnotatedMember)
                .map(annotated -> (AnnotatedMember) annotated)
                .flatMap(annotatedMember ->
                        Optional.ofNullable(annotatedMember.getAnnotation(RosettaAttribute.class))
                                .flatMap(rosettaAttributeAnnotation ->
                                        getTypeXMLConfigurations(config, getEnclosingAnnotatedClass(config, annotatedMember)).stream()
                                                .filter(t -> t.getAttributes().isPresent())
                                                .map(t -> t.getAttributes().get())
                                                .filter(attrMap -> attrMap.containsKey(rosettaAttributeAnnotation.value()))
                                                .map(attributeMap -> attributeMap.get(rosettaAttributeAnnotation.value()))
                                                .findFirst())

                );

    }

    private List<TypeXMLConfiguration> getTypeXMLConfigurations(MapperConfig<?> config, AnnotatedClass ac) {
        List<TypeXMLConfiguration> result = new ArrayList<>();
        Set<ModelSymbolId> visited = new HashSet<>();
        RosettaDataType ann;
        while ((ann = ac.getAnnotation(RosettaDataType.class)) != null) {
            final ModelSymbolId modelSymbolId = createModelSymbolId(ac, ann.value());
            if (visited.add(modelSymbolId)) {
                rosettaXMLConfiguration.getConfigurationForType(modelSymbolId).ifPresent(result::add);
            }

            if (ac.getType().getSuperClass() == null) {
                break;
            }
            ac = AnnotatedClassResolver.resolve(config, ac.getType().getSuperClass(), config);
        }
        return result;
    }
    private Optional<TypeXMLConfiguration> getEnumXMLConfigurations(MapperConfig<?> config, AnnotatedClass ac) {
        RosettaEnum ann = ac.getAnnotation(RosettaEnum.class);
        if (ann != null) {
            final ModelSymbolId modelSymbolId = createModelSymbolId(ac, ann.value());
            return rosettaXMLConfiguration.getConfigurationForType(modelSymbolId);
        }
        return Optional.empty();
    }

    private ModelSymbolId createModelSymbolId(AnnotatedClass ac, String name) {
        final String namespace = ac.getType().getRawClass().getPackage().getName();
        return new ModelSymbolId(DottedPath.splitOnDots(namespace), name);
    }
}
