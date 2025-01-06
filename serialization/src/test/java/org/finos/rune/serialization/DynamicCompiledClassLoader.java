package org.finos.rune.serialization;

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

import java.util.Map;

public class DynamicCompiledClassLoader extends ClassLoader {
    private Map<String, Class<?>> compiledCode;

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return compiledCode.containsKey(name) ? compiledCode.get(name) : super.findClass(name);
    }

    public void setCompiledCode(Map<String, Class<?>> compiledCode) {
        this.compiledCode = compiledCode;
    }
}
