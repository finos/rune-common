package org.finos.rune.mapper.processor;

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

import com.rosetta.model.lib.path.RosettaPath;

import java.util.Objects;

public class GlobalReferenceRecord {
    public final Class<?> referenceOnType;
    public final String referenceKeyValue;

    public GlobalReferenceRecord(Class<?> referenceOnType, String referenceKeyValue) {
        this.referenceOnType = referenceOnType;
        this.referenceKeyValue = referenceKeyValue;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        GlobalReferenceRecord that = (GlobalReferenceRecord) o;
        return Objects.equals(referenceOnType, that.referenceOnType) && Objects.equals(referenceKeyValue, that.referenceKeyValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(referenceOnType, referenceKeyValue);
    }
}
