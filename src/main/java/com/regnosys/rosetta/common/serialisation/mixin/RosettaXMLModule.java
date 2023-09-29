package com.regnosys.rosetta.common.serialisation.mixin;

import com.fasterxml.jackson.core.json.PackageVersion;
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
        super(PackageVersion.VERSION);
        this.rosettaXMLConfiguration = rosettaXMLConfiguration;
        this.supportNativeEnumValue = supportNativeEnumValue;
    }

    @Override
    public void setupModule(SetupContext context) {
        super.setupModule(context);
        context.insertAnnotationIntrospector(new RosettaXMLAnnotationIntrospector(rosettaXMLConfiguration,supportNativeEnumValue));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }
}
