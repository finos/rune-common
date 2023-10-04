package com.regnosys.rosetta.common.serialisation.mixin;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.rosetta.util.serialisation.RosettaXMLConfiguration;

/**
 * Using a module class to append our annotation introspector with a minimal fuss
 */
public class RosettaXMLModule extends SimpleModule {

    private static final long serialVersionUID = 1L;

    private final RosettaXMLConfiguration rosettaXMLConfiguration;

    private final boolean supportNativeEnumValue;


    public RosettaXMLModule(final RosettaXMLConfiguration rosettaXMLConfiguration, final boolean supportNativeEnumValue) {
        super(RosettaXMLModule.class.getSimpleName());
        this.rosettaXMLConfiguration = rosettaXMLConfiguration;
        this.supportNativeEnumValue = supportNativeEnumValue;
    }

    @Override
    public void setupModule(SetupContext context) {
        context.insertAnnotationIntrospector(new RosettaXMLAnnotationIntrospector(rosettaXMLConfiguration,supportNativeEnumValue));
        super.setupModule(context);
    }
}
