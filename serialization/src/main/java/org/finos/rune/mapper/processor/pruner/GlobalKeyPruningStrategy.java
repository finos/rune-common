package org.finos.rune.mapper.processor.pruner;

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

import com.rosetta.model.lib.GlobalKey;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.meta.FieldWithMeta;
import com.rosetta.model.lib.meta.GlobalKeyFields;
import org.finos.rune.mapper.processor.GlobalReferenceRecord;

import java.util.Set;

public class GlobalKeyPruningStrategy implements PruningStrategy {
    private final Set<GlobalReferenceRecord> globalReferences;

    public GlobalKeyPruningStrategy(Set<GlobalReferenceRecord> globalReferences) {
        this.globalReferences = globalReferences;
    }

    @Override
    public void prune(RosettaModelObjectBuilder builder) {
        if (builder instanceof GlobalKey.GlobalKeyBuilder) {
            GlobalKey.GlobalKeyBuilder globalKeyBuilder = (GlobalKey.GlobalKeyBuilder) builder;
            GlobalKeyFields.GlobalKeyFieldsBuilder globalKeyFields = globalKeyBuilder.getMeta();
            String globalKey = globalKeyFields.getGlobalKey();
            if (globalKey != null) {
                GlobalReferenceRecord globalReferenceRecord = new GlobalReferenceRecord(getType(builder), globalKey);
                if (!globalReferences.contains(globalReferenceRecord)) {
                    globalKeyFields.setGlobalKey(null);
                }
            }
        }
    }

    private Class<?> getType(RosettaModelObjectBuilder builder) {
        if (builder instanceof FieldWithMeta.FieldWithMetaBuilder) {
            return ((FieldWithMeta.FieldWithMetaBuilder<?>)builder).getValueType();
        }
        return builder.getType();
    }

}
