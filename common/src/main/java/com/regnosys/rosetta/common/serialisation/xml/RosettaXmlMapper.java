package com.regnosys.rosetta.common.serialisation.xml;

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

import com.fasterxml.jackson.databind.deser.BeanDeserializerFactory;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.regnosys.rosetta.common.serialisation.xml.deserialization.RosettaXmlDeserializationContext;
import com.regnosys.rosetta.common.serialisation.xml.serialization.RosettaSerialiserFactory;

/**
 * This class exists just to override the `_deserializationContext` property.
 */
public class RosettaXmlMapper extends XmlMapper {
    public RosettaXmlMapper() {
        // See issue https://github.com/FasterXML/jackson-dataformat-xml/issues/678
        super((JacksonXmlModule) null);
        this._serializerFactory = RosettaSerialiserFactory.INSTANCE;
        this._deserializationContext = new RosettaXmlDeserializationContext(BeanDeserializerFactory.instance);
    }
}
