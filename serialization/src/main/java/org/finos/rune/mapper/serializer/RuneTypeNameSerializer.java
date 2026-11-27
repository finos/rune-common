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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.finos.rune.mapper.RuneTypeNameConverter;

import java.io.IOException;

/**
 * Writes a generated Java class as the qualified name of its Rune type.
 */
@SuppressWarnings("rawtypes")
public class RuneTypeNameSerializer extends StdSerializer<Class> {

    public RuneTypeNameSerializer() {
        super(Class.class);
    }

    @Override
    public void serialize(Class value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(RuneTypeNameConverter.toRuneName(value));
    }
}
