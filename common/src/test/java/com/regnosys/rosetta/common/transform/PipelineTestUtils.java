package com.regnosys.rosetta.common.transform;

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

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.functions.RosettaFunction;

public class PipelineTestUtils {
    static class Enrich_Type_1ToType_2 implements RosettaFunction {
        public String evaluate(RosettaModelObject input) {
            return "Enriched" + input;
        }
    }

    static class Report_Type_2ToType_3 implements RosettaFunction {
    }

    static class Project_Type_3ToType_4 implements RosettaFunction {
    }

    static class Report_TYPE_2_TO_TYPE_3 implements RosettaFunction {
        public String evaluate(RosettaModelObject input) {
            return "Enriched" + input;
        }
    }
}
