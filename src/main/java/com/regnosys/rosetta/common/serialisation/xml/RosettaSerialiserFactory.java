package com.regnosys.rosetta.common.serialisation.xml;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.SerializerFactoryConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.*;
import com.fasterxml.jackson.databind.ser.impl.IndexedListSerializer;
import com.fasterxml.jackson.databind.ser.std.MapSerializer;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.util.NativeImageUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RosettaSerialiserFactory extends BeanSerializerFactory {
    /**
     * Like {@link BeanSerializerFactory}, this factory is stateless, and
     * thus a single shared global (== singleton) instance can be used
     * without thread-safety issues.
     */
    public final static RosettaSerialiserFactory INSTANCE = new RosettaSerialiserFactory(null);

    protected RosettaSerialiserFactory(SerializerFactoryConfig config) {
        super(config);
    }

    @Override
    public SerializerFactory withConfig(SerializerFactoryConfig config) {
        if (_factoryConfig == config) {
            return this;
        }
        if (getClass() != RosettaSerialiserFactory.class) {
            throw new IllegalStateException("Subtype of RosettaSerialiserFactory (" + getClass().getName()
                    + ") has not properly overridden method 'withAdditionalSerializers': cannot instantiate subtype with "
                    + "additional serializer definitions");
        }
        return new RosettaSerialiserFactory(config);
    }

    @Override
    public  ContainerSerializer<?> buildIndexedListSerializer(JavaType elemType,
                                                              boolean staticTyping, TypeSerializer vts, JsonSerializer<Object> valueSerializer) {
        return new UnwrappableIndexedListSerializer(elemType, staticTyping, vts, valueSerializer);
    }
//
//    protected RosettaBeanSerialiserBuilder constructRosettaBeanSerializerBuilder(BeanDescription beanDesc) {
//        return new RosettaBeanSerialiserBuilder(beanDesc);
//    }

//    @Override
//    protected JsonSerializer<Object> constructBeanOrAddOnSerializer(SerializerProvider prov,
//                                                                    JavaType type, BeanDescription beanDesc, boolean staticTyping)
//            throws JsonMappingException {
//        if (beanDesc instanceof RosettaBeanDescription) {
//            return constructRosettaBeanOrAddOnSerializer(prov, type, (RosettaBeanDescription) beanDesc, staticTyping);
//        }
//        return super.constructBeanOrAddOnSerializer(prov, type, beanDesc, staticTyping);
//    }
//
//    @SuppressWarnings("unchecked")
//    protected JsonSerializer<Object> constructRosettaBeanOrAddOnSerializer(SerializerProvider prov,
//                                                                    JavaType type, RosettaBeanDescription beanDesc, boolean staticTyping)
//            throws JsonMappingException {
//        JsonSerializer<?> ser;
//
//        final SerializationConfig config = prov.getConfig();
//        RosettaBeanSerialiserBuilder builder = constructRosettaBeanSerializerBuilder(beanDesc);
//        builder.setConfig(config);
//
//        // First: any detectable (auto-detect, annotations) properties to serialize?
//        List<BeanPropertyWriter> props = findBeanProperties(prov, beanDesc, builder);
//        if (props == null) {
//            props = new ArrayList<>();
//        } else {
//            props = removeOverlappingTypeIds(prov, beanDesc, builder, props);
//        }
//
//        // [databind#638]: Allow injection of "virtual" properties:
//        prov.getAnnotationIntrospector().findAndAddVirtualProperties(config, beanDesc.getClassInfo(), props);
//
//        // [JACKSON-440] Need to allow modification bean properties to serialize:
//        if (_factoryConfig.hasSerializerModifiers()) {
//            for (BeanSerializerModifier mod : _factoryConfig.serializerModifiers()) {
//                props = mod.changeProperties(config, beanDesc, props);
//            }
//        }
//
//        // Any properties to suppress?
//
//        // 10-Dec-2021, tatu: [databind#3305] Some JDK types need special help
//        //    (initially, `CharSequence` with its `isEmpty()` default impl)
//        props = filterUnwantedJDKProperties(config, beanDesc, props);
//        props = filterBeanProperties(config, beanDesc, props);
//
//        // Need to allow reordering of properties to serialize
//        if (_factoryConfig.hasSerializerModifiers()) {
//            for (BeanSerializerModifier mod : _factoryConfig.serializerModifiers()) {
//                props = mod.orderProperties(config, beanDesc, props);
//            }
//        }
//
//        // And if Object Id is needed, some preparation for that as well: better
//        // do before view handling, mostly for the custom id case which needs
//        // access to a property
//        builder.setObjectIdWriter(constructObjectIdHandler(prov, beanDesc, props));
//
//        builder.setProperties(props);
//        builder.setFilterId(findFilterId(config, beanDesc));
//
//        AnnotatedMember anyGetter = beanDesc.findAnyGetter();
//        if (anyGetter != null) {
//            JavaType anyType = anyGetter.getType();
//            // copied from BasicSerializerFactory.buildMapSerializer():
//            JavaType valueType = anyType.getContentType();
//            TypeSerializer typeSer = createTypeSerializer(config, valueType);
//            // last 2 nulls; don't know key, value serializers (yet)
//            // 23-Feb-2015, tatu: As per [databind#705], need to support custom serializers
//            JsonSerializer<?> anySer = findSerializerFromAnnotation(prov, anyGetter);
//            if (anySer == null) {
//                // TODO: support '@JsonIgnoreProperties' with any setter?
//                anySer = MapSerializer.construct(/* ignored props*/ (Set<String>) null,
//                        anyType, config.isEnabled(MapperFeature.USE_STATIC_TYPING),
//                        typeSer, null, null, /*filterId*/ null);
//            }
//            // TODO: can we find full PropertyName?
//            PropertyName name = PropertyName.construct(anyGetter.getName());
//            BeanProperty.Std anyProp = new BeanProperty.Std(name, valueType, null,
//                    anyGetter, PropertyMetadata.STD_OPTIONAL);
//            builder.setAnyGetter(new AnyGetterWriter(anyProp, anyGetter, anySer));
//        }
//        // Next: need to gather view information, if any:
//        processViews(config, builder);
//
//        // Finally: let interested parties mess with the result bit more...
//        if (_factoryConfig.hasSerializerModifiers()) {
//            for (BeanSerializerModifier mod : _factoryConfig.serializerModifiers()) {
//                builder = (RosettaBeanSerialiserBuilder) mod.updateBuilder(config, beanDesc, builder);
//            }
//        }
//
//        try {
//            ser = builder.build();
//        } catch (RuntimeException e) {
//            return prov.reportBadTypeDefinition(beanDesc, "Failed to construct BeanSerializer for %s: (%s) %s",
//                    beanDesc.getType(), e.getClass().getName(), e.getMessage());
//        }
//        if (ser == null) { // Means that no properties were found
//            // 21-Aug-2020, tatu: Empty Records should be fine tho
//            // 18-Mar-2022, yawkat: [databind#3417] Record will also appear empty when missing
//            // reflection info. needsReflectionConfiguration will check that a constructor is present,
//            // else we fall back to the empty bean error msg
//            if (type.isRecordType() && !NativeImageUtil.needsReflectionConfiguration(type.getRawClass())) {
//                return builder.createDummy();
//            }
//
//            // 06-Aug-2019, tatu: As per [databind#2390], we need to check for add-ons here,
//            //    before considering fallbacks
//            ser = findSerializerByAddonType(config, type, beanDesc, staticTyping);
//            if (ser == null) {
//                // If we get this far, there were no properties found, so no regular BeanSerializer
//                // would be constructed. But, couple of exceptions.
//                // First: if there are known annotations, just create 'empty bean' serializer
//                if (beanDesc.hasKnownClassAnnotations()) {
//                    return builder.createDummy();
//                }
//            }
//        }
//        return (JsonSerializer<Object>) ser;
//    }
//
//    @Override
//    protected JsonSerializer<?> buildCollectionSerializer(SerializerProvider prov,
//                                                          CollectionType type, BeanDescription beanDesc, boolean staticTyping,
//                                                          TypeSerializer elementTypeSerializer, JsonSerializer<Object> elementValueSerializer)
//            throws JsonMappingException {
//        return super.buildCollectionSerializer(prov, type, beanDesc, staticTyping, elementTypeSerializer, elementValueSerializer);
//    }
}
