package com.regnosys.rosetta.common.model;

/*-
 * #%L
 * Rosetta Common
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

import com.rosetta.model.lib.functions.RosettaFunction;

import java.util.*;

public class FunctionMemoisingModuleBuilder {
    public static final String DEBUG_FUNCTION_ENV_PREFIX = "DEBUG_ROSETTA_FUNCTION_";
    private final Set<String> packages = new HashSet<>();
    private final Set<String> debugFunctions = new HashSet<>();

    public FunctionMemoisingModuleBuilder setPackages(String... packages) {
        this.packages.addAll(Arrays.asList(packages));
        return this;
    }

    public FunctionMemoisingModuleBuilder setDebugLoggingFunctions(Class<? extends RosettaFunction>... debugFunctions) {
        Arrays.stream(debugFunctions).map(Class::getSimpleName).map(String::toUpperCase).
                forEach(this.debugFunctions::add);
        return this;
    }

    public FunctionMemoisingModuleBuilder setFromMap(Map<String, String> map) {
        map.entrySet().stream()
                .filter(x -> x.getKey().toUpperCase().startsWith(DEBUG_FUNCTION_ENV_PREFIX))
                .map(x -> new AbstractMap.SimpleEntry<>(x.getKey().toUpperCase()
                        .replace(DEBUG_FUNCTION_ENV_PREFIX, ""), Boolean.parseBoolean(x.getValue())))
                .forEach(e -> {
                    if (Boolean.TRUE.equals(e.getValue())) {
                        debugFunctions.add(e.getKey());
                    } else {
                        debugFunctions.remove(e.getKey());
                    }
                });
        return this;
    }

    public FunctionMemoisingModuleBuilder setFromEnvironment() {
        setFromMap(System.getenv());
        return this;
    }


    public FunctionMemoisingModule build() {
        return new FunctionMemoisingModule(packages, debugFunctions);
    }
}
