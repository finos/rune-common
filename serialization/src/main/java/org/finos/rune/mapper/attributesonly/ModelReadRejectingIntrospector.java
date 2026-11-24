package org.finos.rune.mapper.attributesonly;

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
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;
import com.rosetta.model.lib.RosettaModelObject;

import java.io.IOException;

/**
 * Gives every model type a deserializer that refuses to read it. Attributes-only Rune JSON has no
 * {@code @type} to resolve choice options and subtypes, and no {@code @key}/{@code @ref} to restore references,
 * so reading it would build the wrong objects. Other reads, such as a JSON tree, still work.
 * <p>
 * Jackson asks the introspector for a deserializer before any other source, on every read path
 * ({@code readValue}, {@code convertValue}, {@code ObjectReader}, {@code readValues}), so it must be the
 * primary deserialization introspector.
 */
public class ModelReadRejectingIntrospector extends NopAnnotationIntrospector {

    private static final long serialVersionUID = 1L;

    @Override
    public Object findDeserializer(Annotated annotated) {
        if (annotated instanceof AnnotatedClass && RosettaModelObject.class.isAssignableFrom(annotated.getRawType())) {
            return new ModelReadRejectingDeserializer(annotated.getRawType());
        }
        return null;
    }

    /**
     * Fails when it is resolved, which happens before any content is read, so an empty list of model
     * objects is refused too.
     */
    static class ModelReadRejectingDeserializer extends StdDeserializer<Object> implements ContextualDeserializer {

        private static final long serialVersionUID = 1L;

        ModelReadRejectingDeserializer(Class<?> modelType) {
            super(modelType);
        }

        @Override
        public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
            return ctxt.reportBadDefinition(ctxt.constructType(handledType()), message());
        }

        @Override
        public Object deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
            return ctxt.reportBadDefinition(ctxt.constructType(handledType()), message());
        }

        private String message() {
            return "Cannot read " + handledType().getName() + ": this RuneJsonObjectMapper writes attributes-only "
                    + "Rune JSON, which has no @type or reference metadata to read a model object back from. Read "
                    + "Rune JSON with a RuneJsonObjectMapper that is not attributes-only.";
        }
    }
}
