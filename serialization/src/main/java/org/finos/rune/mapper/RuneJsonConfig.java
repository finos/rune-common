package org.finos.rune.mapper;

/*-
 * ==============
 * Rune Common
 * ==============
 * Copyright (C) 2018 - 2025 REGnosys
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

import java.util.Arrays;
import java.util.List;

import static org.finos.rune.mapper.RuneJsonConfig.MetaProperties.*;

public class RuneJsonConfig {
    public static class MetaProperties {
        public static final String MODEL = "@model";
        public static final String TYPE = "@type";
        public static final String VERSION = "@version";

        private MetaProperties() {}
    }

    private RuneJsonConfig() {}

    public static List<String> getMetaProperties() {
        return Arrays.asList(MODEL, TYPE, VERSION);
    }
}
