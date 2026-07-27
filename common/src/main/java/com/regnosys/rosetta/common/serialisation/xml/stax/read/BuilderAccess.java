package com.regnosys.rosetta.common.serialisation.xml.stax.read;

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

import com.regnosys.rosetta.common.serialisation.xml.stax.introspect.AttributeBinding;
import com.regnosys.rosetta.common.serialisation.xml.stax.introspect.TypeBinding;

/**
 * Shared reflection entry points onto a Rune builder, used by both {@link StaxReader} and
 * {@link VirtualPathAssembler}.
 */
final class BuilderAccess {

    private BuilderAccess() {
    }

    /**
     * Instantiates the generated builder for a type. Builder impl classes are inner classes of the
     * Rune interface and therefore implicitly static, so no enclosing instance is needed.
     */
    static Object newBuilder(TypeBinding binding) throws Exception {
        return binding.getBuilderType().getDeclaredConstructor().newInstance();
    }

    /**
     * Sets or adds a value on the builder: the adder for multi-cardinality attributes, the setter
     * for single-cardinality ones. A {@code null} value is a no-op.
     */
    static void apply(Object builder, AttributeBinding attribute, Object value) throws Exception {
        if (value == null) {
            return;
        }
        if (attribute.isMulti()) {
            if (attribute.getAdder() != null) {
                attribute.getAdder().invoke(builder, value);
            }
        } else {
            if (attribute.getSetter() != null) {
                attribute.getSetter().invoke(builder, value);
            }
        }
    }

    /** Finds an attribute by its logical Rune name, or {@code null} when the type has no such attribute. */
    static AttributeBinding findByLogicalName(TypeBinding binding, String logicalName) {
        for (AttributeBinding attribute : binding.getAttributes()) {
            if (attribute.getLogicalName().equals(logicalName)) {
                return attribute;
            }
        }
        return null;
    }
}
