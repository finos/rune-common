package org.finos.rune.mapper.serializer;

/*-
 * ==============
 * Rune Common
 * ==============
 * Copyright (C) 2018 - 2026 REGnosys
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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.std.DelegatingDeserializer;
import com.rosetta.model.lib.RosettaModelObject;

import java.io.IOException;

public class PruningDeserializer<T> extends DelegatingDeserializer {
    private static final long serialVersionUID = 1L;

    public PruningDeserializer(JsonDeserializer<?> delegatee) {
        super(delegatee);
    }

    @Override
    protected JsonDeserializer<?> newDelegatingInstance(JsonDeserializer<?> newDelegatee) {
        return new PruningDeserializer<>(newDelegatee);
    }

    @Override
    public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        @SuppressWarnings("unchecked")
        T obj = (T) _delegatee.deserialize(p, ctxt);

        if (obj instanceof RosettaModelObject) {
            @SuppressWarnings("unchecked")
            T prunedObject = (T) ((RosettaModelObject) obj).toBuilder().prune().build();
            return prunedObject;
        }

        return obj;
    }
}
