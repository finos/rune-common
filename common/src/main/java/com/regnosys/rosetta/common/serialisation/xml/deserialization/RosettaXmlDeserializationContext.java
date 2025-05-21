package com.regnosys.rosetta.common.serialisation.xml.deserialization;

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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.cfg.CacheProvider;
import com.fasterxml.jackson.databind.deser.DefaultDeserializationContext;
import com.fasterxml.jackson.databind.deser.DeserializerCache;
import com.fasterxml.jackson.databind.deser.DeserializerFactory;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.fasterxml.jackson.dataformat.xml.deser.XmlDeserializationContext;

import java.io.IOException;

/**
 * Copy of {@link XmlDeserializationContext}, with one additional override for method `bufferForInputBuffering`
 * to return our custom {@link UnwrappableTokenBuffer}.
 */
public class RosettaXmlDeserializationContext extends DefaultDeserializationContext {
    private static final long serialVersionUID = 1L;

    /*
    /**********************************************************
    /* Life-cycle methods
    /**********************************************************
     */

    /**
     * Default constructor for a blueprint object, which will use the standard
     * {@link DeserializerCache}, given factory.
     */
    public RosettaXmlDeserializationContext(DeserializerFactory df) {
        // 04-Sep-2023, tatu: Not ideal (wrt not going via CacheProvider) but
        //     has to do for backwards compatibility:
        super(df, new DeserializerCache());
    }

    private RosettaXmlDeserializationContext(RosettaXmlDeserializationContext src,
                                      DeserializationConfig config, JsonParser p, InjectableValues values) {
        super(src, config, p, values);
    }

    private RosettaXmlDeserializationContext(RosettaXmlDeserializationContext src) { super(src); }

    private RosettaXmlDeserializationContext(RosettaXmlDeserializationContext src, DeserializerFactory factory) {
        super(src, factory);
    }

    private RosettaXmlDeserializationContext(RosettaXmlDeserializationContext src, DeserializationConfig config) {
        super(src, config);
    }

    // @since 2.16
    private RosettaXmlDeserializationContext(RosettaXmlDeserializationContext src, CacheProvider cp) {
        super(src, cp);
    }

    @Override
    public TokenBuffer bufferForInputBuffering(JsonParser p) {
        return new UnwrappableTokenBuffer(p, this);
    }
    
    @Override
    public RosettaXmlDeserializationContext copy() {
        return new RosettaXmlDeserializationContext(this);
    }

    @Override
    public DefaultDeserializationContext createInstance(DeserializationConfig config,
                                                        JsonParser p, InjectableValues values) {
        return new RosettaXmlDeserializationContext(this, config, p, values);
    }

    @Override
    public DefaultDeserializationContext createDummyInstance(DeserializationConfig config) {
        // need to be careful to create non-blue-print instance
        return new RosettaXmlDeserializationContext(this, config);
    }

    @Override
    public DefaultDeserializationContext with(DeserializerFactory factory) {
        return new RosettaXmlDeserializationContext(this, factory);
    }

    @Override
    public DefaultDeserializationContext withCaches(CacheProvider cp) {
        return new RosettaXmlDeserializationContext(this, cp);
    }

    /*
    /**********************************************************
    /* Overrides we need
    /**********************************************************
     */

    @Override // since 2.12
    public Object readRootValue(JsonParser p, JavaType valueType,
                                JsonDeserializer<Object> deser, Object valueToUpdate)
            throws IOException
    {
        // 18-Sep-2021, tatu: Complicated mess; with 2.12, had [dataformat-xml#374]
        //    to disable handling. With 2.13, via [dataformat-xml#485] undid this change
        if (_config.useRootWrapping()) {
            return _unwrapAndDeserialize(p, valueType, deser, valueToUpdate);
        }
        if (valueToUpdate == null) {
            return deser.deserialize(p, this);
        }
        return deser.deserialize(p, this, valueToUpdate);
    }

    // To support case where XML element has attributes as well as CDATA, need
    // to "extract" scalar value (CDATA), after the fact
    @Override // since 2.12
    public String extractScalarFromObject(JsonParser p, JsonDeserializer<?> deser,
                                          Class<?> scalarType)
            throws IOException
    {
        // Only called on START_OBJECT, should not need to check, but JsonParser we
        // get may or may not be `FromXmlParser` so traverse using regular means
        String text = "";

        while (p.nextToken() == JsonToken.FIELD_NAME) {
            // Couple of ways to find "real" textual content. One is to look for
            // "XmlText"... but for that would need to know configuration. Alternatively
            // could hold on to last text seen -- but this might be last attribute, for
            // empty element. So for now let's simply hard-code check for empty String
            // as marker and hope for best
            final String propName = p.currentName();
            JsonToken t = p.nextToken();
            if (t == JsonToken.VALUE_STRING) {
                if (propName.equals("")) {
                    text = p.getText();
                }
            } else {
                p.skipChildren();
            }
        }
        return text;
    }
}
