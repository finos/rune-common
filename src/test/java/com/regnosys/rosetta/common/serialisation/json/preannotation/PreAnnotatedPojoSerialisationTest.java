package com.regnosys.rosetta.common.serialisation.json.preannotation;

/*-
 * ==============
 * Rosetta Common
 * --------------
 * Copyright (C) 2018 - 2024 REGnosys
 * --------------
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

/*-
 * #%L
 * Rosetta Common
 * %%
 * Copyright (C) 2018 - 2024 REGnosys
 * %%
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
 * #L%
 */

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapper;
import com.regnosys.rosetta.common.serialisation.json.preannotation.testpojo.PriceQuantity;
import com.regnosys.rosetta.common.serialisation.json.preannotation.testpojo.ResolvablePriceQuantity;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.*;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Optional.ofNullable;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PreAnnotatedPojoSerialisationTest {


    @Test
    void testSerialisationWithAddressLocation() throws JsonProcessingException {
        ObjectMapper mapper = RosettaObjectMapper.getNewRosettaObjectMapper();

        String expectedResolvablePriceQuantityJson = "{\"resolvedPrice\":{\"address\":{\"scope\":\"DOC\",\"value\":\"price-1\"}}}";
        ResolvablePriceQuantity actualResolvablePriceQuantity = mapper.readValue(expectedResolvablePriceQuantityJson, ResolvablePriceQuantity.class);
        String actualResolvablePriceQuantityJson = mapper.writeValueAsString(actualResolvablePriceQuantity);

        assertEquals(expectedResolvablePriceQuantityJson, actualResolvablePriceQuantityJson);

        String expectedPriceQuantityJson = "{\"price\":{\"meta\":{\"location\":[{\"scope\":\"DOC\",\"value\":\"price-1\"}]},\"value\":{\"rate\":999}}}";
        PriceQuantity actualPriceQuantity = mapper.readValue(expectedPriceQuantityJson, PriceQuantity.class);
        String actualPriceQuantityJson = mapper.writeValueAsString(actualPriceQuantity);

        assertEquals(expectedPriceQuantityJson, actualPriceQuantityJson);
    }

    @Test
    void testLegacyAnnotatedPojo() throws JsonProcessingException {
        ObjectMapper mapper = RosettaObjectMapper.getNewRosettaObjectMapper();

        String expectedJson =   "{\"a\":\"A/1\"}";

        PreAnnotation expectedPreAnnotation = PreAnnotation.builder().setA(A.A1).build();

        String actual = mapper.writeValueAsString(expectedPreAnnotation);
        assertEquals(expectedJson, actual);

        PreAnnotation actualPreAnnotation = mapper.readValue(actual, PreAnnotation.class);
        assertEquals(expectedPreAnnotation, actualPreAnnotation);
    }


    enum A {
        A1("A1", "A/1")
        ;
        private static Map<String, A> values;
        static {
            Map<String, A> map = new ConcurrentHashMap<>();
            for (A instance : A.values()) {
                map.put(instance.toDisplayString(), instance);
            }
            values = Collections.unmodifiableMap(map);
        }

        private final String rosettaName;
        private final String displayName;

        A(String rosettaName) {
            this(rosettaName, null);
        }

        A(String rosettaName, String displayName) {
            this.rosettaName = rosettaName;
            this.displayName = displayName;
        }

        public static A fromDisplayName(String name) {
            A value = values.get(name);
            if (value == null) {
                throw new IllegalArgumentException("No enum constant with display name \"" + name + "\".");
            }
            return value;
        }

        @Override
        public String toString() {
            return displayName != null ?  displayName : name();
        }

        public String toDisplayString() {
            return displayName != null ?  displayName : rosettaName;
        }
    }

    @RosettaClass
    interface PreAnnotation extends RosettaModelObject {

        /*********************** Getter Methods  ***********************/
        A getA();

        /*********************** Build Methods  ***********************/
        PreAnnotation build();

        PreAnnotation.PreAnnotationBuilder toBuilder();

        static PreAnnotation.PreAnnotationBuilder builder() {
            return new PreAnnotation.PreAnnotationBuilderImpl();
        }

        /*********************** Utility Methods  ***********************/
        @Override
        default RosettaMetaData<? extends PreAnnotation> metaData() {
            return null;
        }

        @Override
        default Class<? extends PreAnnotation> getType() {
            return PreAnnotation.class;
        }


        @Override
        default void process(RosettaPath path, Processor processor) {
            processor.processBasic(path.newSubPath("a"), A.class, getA(), this);

        }


        /*********************** Builder Interface  ***********************/
        interface PreAnnotationBuilder extends PreAnnotation, RosettaModelObjectBuilder {
            PreAnnotation.PreAnnotationBuilder setA(A a);

            @Override
            default void process(RosettaPath path, BuilderProcessor processor) {

                processor.processBasic(path.newSubPath("a"), A.class, getA(), this);

            }


            PreAnnotation.PreAnnotationBuilder prune();
        }

        /*********************** Immutable Implementation of PreAnnotation  ***********************/
        class PreAnnotationImpl implements PreAnnotation {
            private final A a;

            protected PreAnnotationImpl(PreAnnotation.PreAnnotationBuilder builder) {
                this.a = builder.getA();
            }

            @Override
            public A getA() {
                return a;
            }

            @Override
            public PreAnnotation build() {
                return this;
            }

            @Override
            public PreAnnotation.PreAnnotationBuilder toBuilder() {
                PreAnnotation.PreAnnotationBuilder builder = builder();
                setBuilderFields(builder);
                return builder;
            }

            protected void setBuilderFields(PreAnnotation.PreAnnotationBuilder builder) {
                ofNullable(getA()).ifPresent(builder::setA);
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;

                PreAnnotation _that = getType().cast(o);

                if (!Objects.equals(a, _that.getA())) return false;
                return true;
            }

            @Override
            public int hashCode() {
                int _result = 0;
                _result = 31 * _result + (a != null ? a.getClass().getName().hashCode() : 0);
                return _result;
            }

            @Override
            public String toString() {
                return "PreAnnotation {" +
                        "a=" + this.a +
                        '}';
            }
        }

        /*********************** Builder Implementation of PreAnnotation  ***********************/
        class PreAnnotationBuilderImpl implements PreAnnotation.PreAnnotationBuilder {

            protected A a;

            public PreAnnotationBuilderImpl() {
            }

            @Override
            public A getA() {
                return a;
            }


            @Override
            public PreAnnotation.PreAnnotationBuilder setA(A a) {
                this.a = a==null?null:a;
                return this;
            }

            @Override
            public PreAnnotation build() {
                return new PreAnnotation.PreAnnotationImpl(this);
            }

            @Override
            public PreAnnotation.PreAnnotationBuilder toBuilder() {
                return this;
            }

            @SuppressWarnings("unchecked")
            @Override
            public PreAnnotation.PreAnnotationBuilder prune() {
                return this;
            }

            @Override
            public boolean hasData() {
                if (getA()!=null) return true;
                return false;
            }

            @SuppressWarnings("unchecked")
            @Override
            public PreAnnotation.PreAnnotationBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
                PreAnnotation.PreAnnotationBuilder o = (PreAnnotation.PreAnnotationBuilder) other;


                merger.mergeBasic(getA(), o.getA(), this::setA);
                return this;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;

                PreAnnotation _that = getType().cast(o);

                if (!Objects.equals(a, _that.getA())) return false;
                return true;
            }

            @Override
            public int hashCode() {
                int _result = 0;
                _result = 31 * _result + (a != null ? a.getClass().getName().hashCode() : 0);
                return _result;
            }

            @Override
            public String toString() {
                return "PreAnnotationBuilder {" +
                        "a=" + this.a +
                        '}';
            }
        }
    }



}
