package org.finos.rune.mapper.processor.collector;

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

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.meta.ReferenceWithMeta;
import com.rosetta.model.lib.path.RosettaPath;
import org.finos.rune.mapper.processor.GlobalReferenceRecord;

import java.util.HashSet;
import java.util.Set;

public class GlobalReferenceCollectorStrategy implements CollectorStrategy {
    private final Set<GlobalReferenceRecord> globalReferences = new HashSet<>();

    @Override
    public <R extends RosettaModelObject> void collect(RosettaPath path, R instance) {
        if (instance instanceof ReferenceWithMeta) {
            @SuppressWarnings("unchecked")
            ReferenceWithMeta<R> reference = (ReferenceWithMeta<R>) instance;
            Class<?> referenceValueType = reference.getValueType();
            String referenceKeyValue = reference.getGlobalReference();
            globalReferences.add(new GlobalReferenceRecord(path, referenceValueType, referenceKeyValue));
        }
    }

    public Set<GlobalReferenceRecord> getGlobalReferences() {
        return globalReferences;
    }
}
