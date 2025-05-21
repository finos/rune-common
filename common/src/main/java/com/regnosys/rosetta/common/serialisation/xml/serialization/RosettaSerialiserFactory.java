package com.regnosys.rosetta.common.serialisation.xml.serialization;

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

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.SerializerFactoryConfig;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.*;

/**
 * Make lists unwrappable so each item may be unwrapped as well.
 * See <a href="https://github.com/FasterXML/jackson-dataformat-xml/issues/676">jackson-dataformat-xml#676</a>.
 */
public class RosettaSerialiserFactory extends BeanSerializerFactory {
    /**
     * Like {@link BeanSerializerFactory}, this factory is stateless, and
     * thus a single shared global (== singleton) instance can be used
     * without thread-safety issues.
     */
    public final static RosettaSerialiserFactory INSTANCE = new RosettaSerialiserFactory(null);

    protected RosettaSerialiserFactory(SerializerFactoryConfig config) {
        super(config);
    }

    @Override
    public SerializerFactory withConfig(SerializerFactoryConfig config) {
        if (_factoryConfig == config) {
            return this;
        }
        if (getClass() != RosettaSerialiserFactory.class) {
            throw new IllegalStateException("Subtype of RosettaSerialiserFactory (" + getClass().getName()
                    + ") has not properly overridden method 'withAdditionalSerializers': cannot instantiate subtype with "
                    + "additional serializer definitions");
        }
        return new RosettaSerialiserFactory(config);
    }

    @Override
    public ContainerSerializer<?> buildIndexedListSerializer(JavaType elemType,
                                                             boolean staticTyping, TypeSerializer vts, JsonSerializer<Object> valueSerializer) {
        return new UnwrappableIndexedListSerializer(elemType, staticTyping, vts, valueSerializer);
    }
}
