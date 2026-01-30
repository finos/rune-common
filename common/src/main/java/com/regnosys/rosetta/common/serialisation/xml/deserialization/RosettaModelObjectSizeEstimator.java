package com.regnosys.rosetta.common.serialisation.xml.deserialization;

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

import com.regnosys.rosetta.common.util.SimpleProcessor;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.AttributeMeta;

public class RosettaModelObjectSizeEstimator {
    public static int getNumberOfFields(Object object) {
        if (object == null) {
            return 0;
        }
        if (object instanceof RosettaModelObject) {
            return getNumberOfFields((RosettaModelObject) object);
        }
        return 1;
    }

    public static int getNumberOfFields(RosettaModelObject object) {
        if (object == null) {
            return 0;
        }
        FieldCountProcessor processor = new FieldCountProcessor();
        RosettaPath path = RosettaPath.valueOf(object.getType().getSimpleName());
        object.process(path, processor);
        return processor.getFieldCount();
    }

    private static class FieldCountProcessor extends SimpleProcessor {
        private int fieldCount;

        int getFieldCount() {
            return fieldCount;
        }

        @Override
        public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path, Class<? extends R> rosettaType,
                R instance, RosettaModelObject parent, AttributeMeta... metas) {
            if (instance == null) {
                return false;
            }
            fieldCount++;
            return true;
        }

        @Override
        public <T> void processBasic(RosettaPath path, Class<? extends T> rosettaType, T instance,
                RosettaModelObject parent, AttributeMeta... metas) {
            if (instance != null) {
                fieldCount++;
            }
        }

        @Override
        public Report report() {
            return null;
        }
    }
}
