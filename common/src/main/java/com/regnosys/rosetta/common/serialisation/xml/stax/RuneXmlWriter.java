package com.regnosys.rosetta.common.serialisation.xml.stax;

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

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable, fluent writer configuration for {@link RuneXmlMapper}, mirroring the small slice
 * of Jackson's {@code ObjectWriter} API ({@code withAttribute(...).writeValueAsString(...)})
 * that {@code RosettaObjectMapperCreator.forXML(...)} callers rely on.
 *
 * <p>The one root attribute callers set today is {@code schemaLocation}, which is written as
 * {@code xsi:schemaLocation} on the root element (the {@code xsi} namespace prefix itself comes
 * from the type's constant {@code xmlAttributes} in the XML config, not from here).
 */
public final class RuneXmlWriter {

    public static final String SCHEMA_LOCATION_ATTRIBUTE_NAME = "schemaLocation";
    private static final String XSI_SCHEMA_LOCATION_XML_NAME = "xsi:schemaLocation";

    private final RuneXmlMapper mapper;
    private final boolean prettyPrint;
    private final Map<String, String> rootAttributes;

    RuneXmlWriter(RuneXmlMapper mapper, boolean prettyPrint, Map<String, String> rootAttributes) {
        this.mapper = mapper;
        this.prettyPrint = prettyPrint;
        this.rootAttributes = rootAttributes;
    }

    public RuneXmlWriter withAttribute(String name, String value) {
        Map<String, String> next = new LinkedHashMap<String, String>(rootAttributes);
        next.put(toRootAttributeXmlName(name), value);
        return new RuneXmlWriter(mapper, prettyPrint, next);
    }

    public String writeValueAsString(Object value) throws IOException {
        return mapper.writeValueAsString(value, prettyPrint, rootAttributes);
    }

    private static String toRootAttributeXmlName(String name) {
        return SCHEMA_LOCATION_ATTRIBUTE_NAME.equals(name) ? XSI_SCHEMA_LOCATION_XML_NAME : name;
    }
}
