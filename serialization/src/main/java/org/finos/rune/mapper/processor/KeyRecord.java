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

import java.util.Objects;

public class KeyRecord {
    public final Class<?> keyOnType;
    public final String keyReferenceValue;

    public KeyRecord(Class<?> keyOnType, String keyReferenceValue) {
        this.keyOnType = keyOnType;
        this.keyReferenceValue = keyReferenceValue;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        KeyRecord keyRecord = (KeyRecord) o;
        return Objects.equals(keyOnType, keyRecord.keyOnType) && Objects.equals(keyReferenceValue, keyRecord.keyReferenceValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyOnType, keyReferenceValue);
    }
}
