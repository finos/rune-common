package com.regnosys.rosetta.common.serialisation.mixin;

import com.fasterxml.jackson.core.json.PackageVersion;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.regnosys.rosetta.common.serialisation.mixin.RosettaBuilderIntrospector;

/**
 * Using a module class to append our annotation introspector with a minimal fuss
 */
public class RosettaModule extends SimpleModule {

    private static final long serialVersionUID = 1L;

    public RosettaModule() {
        super(PackageVersion.VERSION);
    }

    @Override
    public void setupModule(SetupContext context) {
        super.setupModule(context);
        context.insertAnnotationIntrospector(new RosettaBuilderIntrospector());
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
