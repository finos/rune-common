package com.regnosys.rosetta.common.hashing;

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

import com.regnosys.rosetta.common.postprocess.testpojo.Price;
import com.regnosys.rosetta.common.postprocess.testpojo.PriceQuantity;
import com.regnosys.rosetta.common.postprocess.testpojo.ResolvablePriceQuantity;
import com.regnosys.rosetta.common.postprocess.testpojo.metafields.FieldWithMetaPrice;
import com.regnosys.rosetta.common.postprocess.testpojo.metafields.ReferenceWithMetaPrice;
import com.rosetta.model.lib.meta.Key;
import com.rosetta.model.lib.meta.Reference;
import com.rosetta.model.metafields.MetaFields;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.regnosys.rosetta.common.postprocess.testpojo.PriceQuantity.PriceQuantityBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class UpdateTemporaryKeyProcessStepTest {

    private static final BigDecimal RATE = BigDecimal.valueOf(1.234);
    private static final Price PRICE = Price.builder()
            .setRate(RATE)
            .build();

    @Test
    void shouldUpdateTemporaryKeyInGlobalKey() {
        // Create a test object with a temporary key
        PriceQuantityBuilder builder = PriceQuantity.builder()
                .addPrice(FieldWithMetaPrice.builder()
                        .setValue(PRICE)
                        .setMeta(MetaFields.builder().toBuilder()
                                .addKey(Key.builder()
                                        .setKeyValue("price-$123456")
                                        .setScope("DOCUMENT"))));

        // Create the processor
        UpdateTemporaryKeyProcessStep processor = new UpdateTemporaryKeyProcessStep(ReferenceConfig.noScopeOrExcludedPaths());

        // Process the object
        processor.runProcessStep(builder.getType(), builder);

        // Verify that the temporary key has been updated
        String updatedKeyValue = builder.getPrice().get(0).getMeta().getKey().get(0).getKeyValue();
        assertNotEquals("price-$123456", updatedKeyValue);
        assertEquals("price-1", updatedKeyValue);
    }

    @Test
    <T> void shouldUpdateTemporaryKeyInReference() {
        // Create a test object with a temporary key in a reference
        ResolvablePriceQuantity.ResolvablePriceQuantityBuilder builder = ResolvablePriceQuantity.builder()
                .setResolvedPrice(ReferenceWithMetaPrice.builder()
                        .setValue(PRICE)
                        .setReference(Reference.builder()
                                .setReference("price-$654321")
                                .setScope("DOCUMENT")));

        // Create the processor
        UpdateTemporaryKeyProcessStep processor = new UpdateTemporaryKeyProcessStep(ReferenceConfig.noScopeOrExcludedPaths());

        // Process the object
        processor.runProcessStep(builder.getType(), builder);

        // Verify that the temporary key has been updated
        String updatedReferenceValue = builder.getResolvedPrice().getReference().getReference();
        assertNotEquals("price-$654321", updatedReferenceValue);
        assertEquals("price-1", updatedReferenceValue);
    }

    @Test
    void shouldUpdateMultipleTemporaryKeysWithSequentialNumbers() {
        // Create a test object with multiple temporary keys with the same prefix
        PriceQuantityBuilder builder = PriceQuantity.builder()
                .addPrice(FieldWithMetaPrice.builder()
                        .setValue(PRICE)
                        .setMeta(MetaFields.builder().toBuilder()
                                .addKey(Key.builder()
                                        .setKeyValue("price-$111")
                                        .setScope("DOCUMENT"))))
                .addPrice(FieldWithMetaPrice.builder()
                        .setValue(PRICE)
                        .setMeta(MetaFields.builder().toBuilder()
                                .addKey(Key.builder()
                                        .setKeyValue("price-$222")
                                        .setScope("DOCUMENT"))));

        // Create the processor
        UpdateTemporaryKeyProcessStep processor = new UpdateTemporaryKeyProcessStep(ReferenceConfig.noScopeOrExcludedPaths());

        // Process the object
        processor.runProcessStep(builder.getType(), builder);

        // Verify that the temporary key has been updated
        String price1KeyValue = builder.getPrice().get(0).getMeta().getKey().get(0).getKeyValue();
        assertEquals("price-1", price1KeyValue);

        // Verify that the temporary key has been updated
        String price2KeyValue = builder.getPrice().get(1).getMeta().getKey().get(0).getKeyValue();
        assertEquals("price-2", price2KeyValue);
    }

    @Test
    void shouldNotUpdateNonTemporaryKeys() {
        // Create a test object with a non-temporary key
        PriceQuantityBuilder builder = PriceQuantity.builder()
                .addPrice(FieldWithMetaPrice.builder()
                        .setValue(PRICE)
                        .setMeta(MetaFields.builder().toBuilder()
                                .addKey(Key.builder()
                                        .setKeyValue("regular-key")
                                        .setScope("DOCUMENT"))));

        // Create the processor
        UpdateTemporaryKeyProcessStep processor = new UpdateTemporaryKeyProcessStep(ReferenceConfig.noScopeOrExcludedPaths());

        // Process the object
        processor.runProcessStep(builder.getType(), builder);

        // Verify that the non-temporary key has not been updated
        String keyValue = builder.getPrice().get(0).getMeta().getKey().get(0).getKeyValue();
        assertEquals("regular-key", keyValue);
    }

    @Test
    void shouldHandleDifferentPrefixes() {
        // Create test objects with different prefixes
        PriceQuantityBuilder builder = PriceQuantity.builder()
                .addPrice(FieldWithMetaPrice.builder()
                        .setValue(PRICE)
                        .setMeta(MetaFields.builder().toBuilder()
                                .addKey(Key.builder()
                                        .setKeyValue("price-$111")
                                        .setScope("DOCUMENT"))))
                .addPrice(FieldWithMetaPrice.builder()
                        .setValue(PRICE)
                        .setMeta(MetaFields.builder().toBuilder()
                                .addKey(Key.builder()
                                        .setKeyValue("quantity-$222")
                                        .setScope("DOCUMENT"))));

        // Create the processor
        UpdateTemporaryKeyProcessStep processor = new UpdateTemporaryKeyProcessStep(ReferenceConfig.noScopeOrExcludedPaths());

        // Process the object
        processor.runProcessStep(builder.getType(), builder);

        // Verify that the temporary key has been updated
        String price1KeyValue = builder.getPrice().get(0).getMeta().getKey().get(0).getKeyValue();
        assertEquals("price-1", price1KeyValue);

        // Verify that the temporary key has been updated
        String quantity1KeyValue = builder.getPrice().get(1).getMeta().getKey().get(0).getKeyValue();
        assertEquals("quantity-1", quantity1KeyValue);
    }
}