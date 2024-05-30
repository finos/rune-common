package com.regnosys.rosetta.common.serialisation.mixin;

/*-
 * #%L
 * Rune Common
 * %%
 * Copyright (C) 2018 - 2024 REGnosys
 * %%
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
 * #L%
 */

import com.fasterxml.jackson.core.json.PackageVersion;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Using a module class to append our annotation introspector with a minimal fuss
 */
public class RosettaJSONModule extends SimpleModule {

    private static final long serialVersionUID = 1L;
    private final boolean supportRosettaEnumValue;

    public RosettaJSONModule(boolean supportRosettaEnumValue) {
        super(PackageVersion.VERSION);
        this.supportRosettaEnumValue = supportRosettaEnumValue;
    }

    @Override
    public void setupModule(SetupContext context) {
        super.setupModule(context);
        context.insertAnnotationIntrospector(new RosettaJSONAnnotationIntrospector(supportRosettaEnumValue));
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
