package com.regnosys.rosetta.common.serialisation.xml;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapperCreator;
import com.regnosys.rosetta.common.serialisation.xml.config.RosettaXMLConfiguration;
import com.regnosys.rosetta.common.serialisation.xml.stax.RuneXmlMapper;

/**
 * Backwards-compatibility alias for {@link StaxXmlObjectMapper}, retained at the fully-qualified
 * name the Jackson-based XML mapper used so that existing imports and constructor calls keep
 * compiling.
 *
 * <p>The XML mapper is now a purpose-built StAX binder rather than a Jackson
 * {@code jackson-dataformat-xml} mapper. Two consequences matter if you are holding a reference to
 * this type:</p>
 * <ul>
 *   <li>It <strong>no longer extends {@code XmlMapper}</strong>, only {@link ObjectMapper} —
 *       {@code jackson-dataformat-xml} is no longer a dependency of this module. Code that assigned
 *       this to an {@code XmlMapper}-typed variable, or fed it to {@code new XmlMapper.Builder(..)},
 *       must be reworked; see below.</li>
 *   <li>Reading and writing bypass Jackson's streaming layer entirely, so overriding
 *       {@code ObjectMapper}'s {@code _readValue} / {@code _readMapAndClose} in a subclass still
 *       compiles but has <strong>no effect</strong> — those hooks are never reached.</li>
 * </ul>
 *
 * <p>Note also that the former {@code RosettaXmlMapper} was never usable on its own: it had to be
 * combined with the (now deleted) {@code RosettaSerialiserFactory} and {@code RosettaXMLModule} to
 * produce Rosetta-aware XML. This class, by contrast, is fully configured on construction.</p>
 *
 * <p><strong>Prefer instead:</strong> {@link RosettaObjectMapperCreator#forXML} for an
 * {@link ObjectMapper}-shaped result, or {@link RuneXmlMapper} for the native, Jackson-free API.</p>
 *
 * @deprecated use {@link RosettaObjectMapperCreator#forXML} or {@link RuneXmlMapper} directly.
 *             Retained only for source compatibility with the deleted Jackson XML engine.
 */
@Deprecated
public class RosettaXmlMapper extends StaxXmlObjectMapper {

    private static final long serialVersionUID = 1L;

    public RosettaXmlMapper(RosettaXMLConfiguration config, ClassLoader classLoader) {
        super(config, classLoader);
    }
}
