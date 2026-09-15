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

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.ResolvableSerializer;
import com.fasterxml.jackson.databind.util.NameTransformer;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.annotations.RuneDataType;
import org.finos.rune.mapper.RuneJsonConfig;
import org.finos.rune.mapper.processor.SerializationPreProcessor;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

public class PreProcessingSerializer<T> extends JsonSerializer<T> implements ContextualSerializer, ResolvableSerializer {
    private final JsonSerializer<T> delegate;
    private final SerializationPreProcessor serializationPreProcessor;
    private static final String MASTER_SNAPSHOT_VERSION = "0.0.0.master-SNAPSHOT";
    private static final String NORMALISE_SAMPLE_VERSION = "NORMALISE_SAMPLE_VERSION";


    public PreProcessingSerializer(JsonSerializer<?> delegate) {
        this(cast(delegate), new SerializationPreProcessor());
    }

    private PreProcessingSerializer(JsonSerializer<T> delegate, SerializationPreProcessor serializationPreProcessor) {
        this.delegate = delegate;
        this.serializationPreProcessor = serializationPreProcessor;
    }

    @Override
    public void serialize(T value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value instanceof RosettaModelObject && gen.getOutputContext().inRoot()) {
            RosettaModelObject processed = serializationPreProcessor.process((RosettaModelObject) value);
            serializers.defaultSerializeValue(createTopLevelHeadersWrapper(processed), gen);
            return;
        }
        delegate.serialize(value, gen, serializers);
    }

    @Override
    public void serializeWithType(T value, JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer) throws IOException {
        if (value instanceof RosettaModelObject && gen.getOutputContext().inRoot()) {
            serialize(value, gen, serializers);
            return;
        }
        delegate.serializeWithType(value, gen, serializers, typeSer);
    }

    @Override
    public boolean isEmpty(SerializerProvider provider, T value) {
        return delegate.isEmpty(provider, value);
    }

    @Override
    public Class<T> handledType() {
        return delegate.handledType();
    }

    @Override
    public JsonSerializer<T> unwrappingSerializer(NameTransformer transformer) {
        return wrap(delegate.unwrappingSerializer(transformer));
    }

    @Override
    public JsonSerializer<T> replaceDelegatee(JsonSerializer<?> delegatee) {
        return wrap(delegatee);
    }

    @Override
    public JsonSerializer<?> withFilterId(Object filterId) {
        return wrap(delegate.withFilterId(filterId));
    }

    @Override
    public JsonSerializer<?> withIgnoredProperties(Set<String> ignoredProperties) {
        return wrap(delegate.withIgnoredProperties(ignoredProperties));
    }

    @Override
    public boolean isUnwrappingSerializer() {
        return delegate.isUnwrappingSerializer();
    }

    @Override
    public JsonSerializer<?> getDelegatee() {
        return delegate;
    }

    @Override
    public Iterator<PropertyWriter> properties() {
        return delegate.properties();
    }

    @Override
    public void resolve(SerializerProvider provider) throws JsonMappingException {
        if (delegate instanceof ResolvableSerializer) {
            ((ResolvableSerializer) delegate).resolve(provider);
        }
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException {
        JsonSerializer<?> serializer = delegate;
        if (serializer instanceof ContextualSerializer) {
            serializer = ((ContextualSerializer) serializer).createContextual(prov, property);
        }
        if (serializer == delegate) {
            return this;
        }
        return wrap(serializer);
    }

    private Object createTopLevelHeadersWrapper(RosettaModelObject rosettaModelObject) {
        Class<? extends RosettaModelObject> runeType = rosettaModelObject.getType();
        return Arrays.stream(runeType.getAnnotations())
                .filter(allAnnotations -> allAnnotations.annotationType().equals(RuneDataType.class))
                .findFirst()
                .<Object>map(a -> {
                    RuneDataType runeDataType = (RuneDataType) a;
                    TopLevel topLevel = new TopLevel();
                    topLevel.setModel(runeDataType.model());
                    topLevel.setType(runeType.getCanonicalName());
                    topLevel.setVersion(getVersion(runeDataType));
                    topLevel.setRosettaModelObject(rosettaModelObject);
                    return topLevel;
                })
                .orElse(rosettaModelObject);
    }

    private String getVersion(RuneDataType runeDataType) {
        String version = runeDataType.version();
        if (Boolean.parseBoolean(System.getenv(NORMALISE_SAMPLE_VERSION))) {
            return MASTER_SNAPSHOT_VERSION;
        }
        return version;
    }

    @SuppressWarnings("unchecked")
    private static <X> JsonSerializer<X> cast(JsonSerializer<?> serializer) {
        return (JsonSerializer<X>) serializer;
    }

    private JsonSerializer<T> wrap(JsonSerializer<?> serializer) {
        if (serializer == delegate) {
            return this;
        }
        return new PreProcessingSerializer<>(cast(serializer), serializationPreProcessor);
    }

    private static class TopLevel {
        private String model;
        private String type;
        private String version;
        private RosettaModelObject rosettaModelObject;

        @JsonGetter(RuneJsonConfig.MetaProperties.MODEL)
        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        @JsonGetter(RuneJsonConfig.MetaProperties.TYPE)
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        @JsonGetter(RuneJsonConfig.MetaProperties.VERSION)
        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        @JsonGetter("rosettaModelObject")
        @JsonUnwrapped
        public RosettaModelObject getRosettaModelObject() {
            return rosettaModelObject;
        }

        public void setRosettaModelObject(RosettaModelObject rosettaModelObject) {
            this.rosettaModelObject = rosettaModelObject;
        }
    }
}
