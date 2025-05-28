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
import com.regnosys.rosetta.common.postprocess.testpojo.ResolvablePriceQuantity;
import com.regnosys.rosetta.common.postprocess.testpojo.metafields.FieldWithMetaPrice;
import com.regnosys.rosetta.common.postprocess.testpojo.metafields.ReferenceWithMetaPrice;
import com.rosetta.model.lib.meta.Key;
import com.rosetta.model.lib.meta.Reference;
import com.rosetta.model.metafields.MetaFields;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

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
        FieldWithMetaPrice object = FieldWithMetaPrice.builder()
                .setValue(PRICE)
                .setMeta(MetaFields.builder().toBuilder()
                        .addKey(Key.builder()
                                .setKeyValue("price-$123456")
                                .setScope("DOCUMENT")))
                .build();

        // Create the processor
        UpdateTemporaryKeyProcessStep processor = new UpdateTemporaryKeyProcessStep(ReferenceConfig.noScopeOrExcludedPaths());
        
        // Process the object
        processor.runProcessStep(object.getClass(), object);
        
        // Verify that the temporary key has been updated
        String updatedKeyValue = object.getMeta().getKey().get(0).getKeyValue();
        assertNotEquals("price-$123456", updatedKeyValue);
        assertEquals("price-1", updatedKeyValue);
    }

    @Test
    void shouldUpdateTemporaryKeyInReference() {
        // Create a test object with a temporary key in a reference
        ResolvablePriceQuantity object = ResolvablePriceQuantity.builder()
                .setResolvedPrice(ReferenceWithMetaPrice.builder()
                        .setValue(PRICE)
                        .setReference(Reference.builder()
                                .setReference("price-$654321")
                                .setScope("DOCUMENT")))
                .build();

        // Create the processor
        UpdateTemporaryKeyProcessStep processor = new UpdateTemporaryKeyProcessStep(ReferenceConfig.noScopeOrExcludedPaths());
        
        // Process the object
        processor.runProcessStep(object.getClass(), object);
        
        // Verify that the temporary key has been updated
        String updatedReferenceValue = object.getResolvedPrice().getReference().getReference();
        assertNotEquals("price-$654321", updatedReferenceValue);
        assertEquals("price-1", updatedReferenceValue);
    }

    @Test
    void shouldUpdateMultipleTemporaryKeysWithSequentialNumbers() {
        // Create a test object with multiple temporary keys with the same prefix
        ResolvablePriceQuantity object = ResolvablePriceQuantity.builder()
                .setResolvedPrice(ReferenceWithMetaPrice.builder()
                        .setValue(PRICE)
                        .setReference(Reference.builder()
                                .setReference("price-$111")
                                .setScope("DOCUMENT")))
                .build();
        
        FieldWithMetaPrice fieldWithMeta = FieldWithMetaPrice.builder()
                .setValue(PRICE)
                .setMeta(MetaFields.builder().toBuilder()
                        .addKey(Key.builder()
                                .setKeyValue("price-$222")
                                .setScope("DOCUMENT")))
                .build();

        // Create the processor
        UpdateTemporaryKeyProcessStep processor = new UpdateTemporaryKeyProcessStep(ReferenceConfig.noScopeOrExcludedPaths());
        
        // Process the objects
        processor.runProcessStep(object.getClass(), object);
        processor.runProcessStep(fieldWithMeta.getClass(), fieldWithMeta);
        
        // Verify that the temporary keys have been updated with sequential numbers
        String updatedReferenceValue = object.getResolvedPrice().getReference().getReference();
        String updatedKeyValue = fieldWithMeta.getMeta().getKey().get(0).getKeyValue();
        
        assertEquals("price-1", updatedReferenceValue);
        assertEquals("price-2", updatedKeyValue);
    }

    @Test
    void shouldNotUpdateNonTemporaryKeys() {
        // Create a test object with a non-temporary key
        FieldWithMetaPrice object = FieldWithMetaPrice.builder()
                .setValue(PRICE)
                .setMeta(MetaFields.builder().toBuilder()
                        .addKey(Key.builder()
                                .setKeyValue("regular-key")
                                .setScope("DOCUMENT")))
                .build();

        // Create the processor
        UpdateTemporaryKeyProcessStep processor = new UpdateTemporaryKeyProcessStep(ReferenceConfig.noScopeOrExcludedPaths());
        
        // Process the object
        processor.runProcessStep(object.getClass(), object);
        
        // Verify that the non-temporary key has not been updated
        String keyValue = object.getMeta().getKey().get(0).getKeyValue();
        assertEquals("regular-key", keyValue);
    }

    @Test
    void shouldHandleDifferentPrefixes() {
        // Create test objects with different prefixes
        FieldWithMetaPrice priceObject = FieldWithMetaPrice.builder()
                .setValue(PRICE)
                .setMeta(MetaFields.builder().toBuilder()
                        .addKey(Key.builder()
                                .setKeyValue("price-$123")
                                .setScope("DOCUMENT")))
                .build();
                
        FieldWithMetaPrice quantityObject = FieldWithMetaPrice.builder()
                .setValue(PRICE)
                .setMeta(MetaFields.builder().toBuilder()
                        .addKey(Key.builder()
                                .setKeyValue("quantity-$456")
                                .setScope("DOCUMENT")))
                .build();

        // Create the processor
        UpdateTemporaryKeyProcessStep processor = new UpdateTemporaryKeyProcessStep(ReferenceConfig.noScopeOrExcludedPaths());
        
        // Process the objects
        processor.runProcessStep(priceObject.getClass(), priceObject);
        processor.runProcessStep(quantityObject.getClass(), quantityObject);
        
        // Verify that the temporary keys have been updated with the correct prefixes
        String priceKeyValue = priceObject.getMeta().getKey().get(0).getKeyValue();
        String quantityKeyValue = quantityObject.getMeta().getKey().get(0).getKeyValue();
        
        assertEquals("price-1", priceKeyValue);
        assertEquals("quantity-1", quantityKeyValue);
    }
}