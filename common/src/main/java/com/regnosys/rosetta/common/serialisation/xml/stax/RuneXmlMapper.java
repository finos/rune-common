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

import com.regnosys.rosetta.common.serialisation.xml.config.RosettaXMLConfiguration;
import com.regnosys.rosetta.common.serialisation.xml.stax.read.StaxReader;
import com.regnosys.rosetta.common.serialisation.xml.stax.write.StaxWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

/**
 * Public, Jackson-free entry point for the StAX-native Rune XML binder.
 *
 * <p>Wraps {@link StaxReader}/{@link StaxWriter} behind a small, dependency-free API for
 * consumers who don't need Jackson's {@code ObjectMapper} shape. {@code RosettaObjectMapperCreator
 * .forXML(...)} wraps this same class behind an {@code ObjectMapper}-compatible facade
 * ({@code StaxXmlObjectMapper}) for existing callers.
 */
public final class RuneXmlMapper {

    private final StaxReader reader;
    private final StaxWriter writer;

    public RuneXmlMapper(RosettaXMLConfiguration config, ClassLoader classLoader) {
        this.reader = new StaxReader(config, classLoader);
        this.writer = new StaxWriter(config);
    }

    public RuneXmlMapper(RosettaXMLConfiguration config) {
        this(config, RuneXmlMapper.class.getClassLoader());
    }

    public <T> T readValue(String xml, Class<T> type) throws IOException {
        try {
            return reader.read(xml, type);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Failed to read XML into " + type.getName(), e);
        }
    }

    public <T> T readValue(Reader source, Class<T> type) throws IOException {
        return readValue(readFully(source), type);
    }

    public <T> T readValue(InputStream source, Class<T> type) throws IOException {
        return readValue(new InputStreamReader(source, StandardCharsets.UTF_8), type);
    }

    public String writeValueAsString(Object value) throws IOException {
        return writeValueAsString(value, false, Collections.<String, String>emptyMap());
    }

    public String writeValueAsString(Object value, boolean prettyPrint, Map<String, String> extraRootAttrs) throws IOException {
        try {
            return writer.write(value, prettyPrint, extraRootAttrs);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Failed to write XML for " + value, e);
        }
    }

    public RuneXmlWriter writer() {
        return new RuneXmlWriter(this, false, Collections.<String, String>emptyMap());
    }

    public RuneXmlWriter writerWithDefaultPrettyPrinter() {
        return new RuneXmlWriter(this, true, Collections.<String, String>emptyMap());
    }

    private static String readFully(Reader source) throws IOException {
        StringWriter sw = new StringWriter();
        char[] buf = new char[4096];
        int n;
        while ((n = source.read(buf)) != -1) {
            sw.write(buf, 0, n);
        }
        return sw.toString();
    }
}
