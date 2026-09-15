package com.regnosys.rosetta.common.serialisation;

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

import com.rosetta.model.lib.transform.SerializationFormat;

import java.util.Objects;

/**
 * The serialization of one side (input or output) of a transform function: a {@link SerializationFormat}
 * plus an optional serialization config file (e.g. an XML config for the {@code XML} format).
 * <p>
 * This is a pure value: it carries no Jackson objects and no {@link ClassLoader}, so it can be resolved
 * anywhere (see {@link TransformSerializationResolver}) and handed to a {@link TransformMapperFactory}
 * for construction wherever the classloader lives.
 */
public final class TransformSerialization {

    /** The default when a transform side carries no serialization at all: plain JSON, no config. */
    public static final TransformSerialization DEFAULT_JSON = new TransformSerialization(SerializationFormat.JSON, null);

    private final SerializationFormat format;
    private final String configPath;

    public TransformSerialization(SerializationFormat format, String configPath) {
        this.format = Objects.requireNonNull(format, "format must not be null");
        this.configPath = configPath;
    }

    public SerializationFormat getFormat() {
        return format;
    }

    /** The classpath location of the serialization config file, or {@code null} when the format needs none. */
    public String getConfigPath() {
        return configPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TransformSerialization)) {
            return false;
        }
        TransformSerialization that = (TransformSerialization) o;
        return format == that.format && Objects.equals(configPath, that.configPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(format, configPath);
    }

    @Override
    public String toString() {
        return "TransformSerialization{format=" + format + ", configPath=" + configPath + "}";
    }
}
