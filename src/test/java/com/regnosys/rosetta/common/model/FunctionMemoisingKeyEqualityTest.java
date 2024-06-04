package com.regnosys.rosetta.common.model;

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

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class FunctionMemoisingKeyEqualityTest {

    static class FooBuilder {
        private String currency;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FooBuilder that = (FooBuilder) o;
            return Objects.equals(currency, that.currency);
        }

        @Override
        public int hashCode() {
            return Objects.hash(currency);
        }

        public FooBuilder append(BigDecimal input) {
            currency = String.format("currency+%s", input.toString());
            return this;
        }

        public FooBuilder append(String input) {
            currency = String.format("currency+%s", input);
            return this;
        }

        public void evaluate(String xxx) {
        }
    }

    @Test
    void checkRosettaModelObjectEquality() {
        MemoiseCacheKey memoiseCacheKey1 = MemoiseCacheKey.create("Foo",
                createFooBuilder("USD"));

        MemoiseCacheKey memoiseCacheKey2 = MemoiseCacheKey.create("Foo",
                createFooBuilder("USD"));

        assertEquals(memoiseCacheKey1, memoiseCacheKey2);
    }

    @Test
    void checkRosettaModelObjectNotEqual() {
        MemoiseCacheKey memoiseCacheKey1 = MemoiseCacheKey.create("Foo",
                createFooBuilder("USD"));

        MemoiseCacheKey memoiseCacheKey2 = MemoiseCacheKey.create("Foo",
                createFooBuilder("GBP"));

        assertNotEquals(memoiseCacheKey1, memoiseCacheKey2);
    }

    @Test
    void checkMethodNotEqual() {
        MemoiseCacheKey memoiseCacheKey1 = MemoiseCacheKey.create("Foo","xxx");
        MemoiseCacheKey memoiseCacheKey2 = MemoiseCacheKey.create("Bar", "xxx");

        assertNotEquals(memoiseCacheKey1, memoiseCacheKey2);
    }


    @Test
    void checkRosettaModelObjectListEquality() {
        MemoiseCacheKey memoiseCacheKey1 = MemoiseCacheKey.create("Foo",
                Arrays.asList(createFooBuilder("USD"), createFooBuilder("GBP")));

        MemoiseCacheKey memoiseCacheKey2 = MemoiseCacheKey.create("Foo",
                Arrays.asList(createFooBuilder("USD"), createFooBuilder("GBP")));

        assertEquals(memoiseCacheKey1, memoiseCacheKey2);
    }

    private static FooBuilder createFooBuilder(String currency) {
        return new FooBuilder().append(currency);
    }

    @Test
    void checkRosettaModelObjectListEqualityAndString() {
        MemoiseCacheKey memoiseCacheKey1 = MemoiseCacheKey.create("Foo",
                "999", Arrays.asList(createFooBuilder("USD"), createFooBuilder("GBP")));

        MemoiseCacheKey memoiseCacheKey2 = MemoiseCacheKey.create("Foo",
                "999", Arrays.asList(createFooBuilder("USD"), createFooBuilder("GBP")));

        assertEquals(memoiseCacheKey1, memoiseCacheKey2);
    }

    @Test
    void checkOutOfOrder() {
        MemoiseCacheKey memoiseCacheKey1 = MemoiseCacheKey.create("Foo",
                "A", "B");

        MemoiseCacheKey memoiseCacheKey2 = MemoiseCacheKey.create("Foo",
                "B", "A");

        assertNotEquals(memoiseCacheKey1, memoiseCacheKey2);
    }

}
