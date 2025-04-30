package com.regnosys.rosetta.common.postprocess;

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
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.metafields.MetaFields;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathCountProcessorTest {

    public static final BigDecimal RATE = BigDecimal.valueOf(1.234);
    public static final Price PRICE = Price.builder()
            .setRate(RATE)
            .build();

    @Test
    void shouldCountAllPaths() {
        PathCountProcessor processor = new PathCountProcessor();
        PRICE.process(new RosettaPath.NullPath(), processor);

        Map<RosettaPath, Object> collectedPaths = processor.report().getCollectedPaths();
        assertEquals(1, collectedPaths.size());

        RosettaPath valuePath = RosettaPath.valueOf("rate");
        assertTrue(collectedPaths.containsKey(valuePath));
        assertEquals(RATE, collectedPaths.get(valuePath));
    }

    @Test
    void shouldCountExternalReferencePathAndNotGlobalReferencePathOrResolvedValuePath() {
        // if metadata reference is specified, then count the paths of the value
        ResolvablePriceQuantity object = ResolvablePriceQuantity.builder()
                .setResolvedPrice(ReferenceWithMetaPrice.builder()
                        .setValue(PRICE)
                        .setGlobalReference("global")
                        .setExternalReference("external"))
                .build();

        PathCountProcessor processor = new PathCountProcessor();
        object.process(new RosettaPath.NullPath(), processor);

        Map<RosettaPath, Object> collectedPaths = processor.report().getCollectedPaths();
        assertEquals(1, collectedPaths.size());

        RosettaPath externalReferencePath = RosettaPath.valueOf("resolvedPrice.externalReference");
        assertTrue(collectedPaths.containsKey(externalReferencePath));
        assertEquals("external", collectedPaths.get(externalReferencePath));
    }

    @Test
    void shouldCountScopedReferencePathAndNotResolvedValuePath() {
        // if metadata reference is specified, then count the paths of the value
        ResolvablePriceQuantity object = ResolvablePriceQuantity.builder()
                .setResolvedPrice(ReferenceWithMetaPrice.builder()
                        .setValue(PRICE)
                        .setReference(Reference.builder().setReference("address")))
                .build();

        PathCountProcessor processor = new PathCountProcessor();
        object.process(new RosettaPath.NullPath(), processor);

        Map<RosettaPath, Object> collectedPaths = processor.report().getCollectedPaths();
        assertEquals(1, collectedPaths.size());

        RosettaPath scopedReferencePath = RosettaPath.valueOf("resolvedPrice.reference.address");
        assertTrue(collectedPaths.containsKey(scopedReferencePath));
        assertEquals("address", collectedPaths.get(scopedReferencePath));
    }

    @Test
    void shouldCountValuePaths() {
        // if no metadata reference is specified, then count the paths of the value
        ResolvablePriceQuantity object = ResolvablePriceQuantity.builder()
                .setResolvedPrice(ReferenceWithMetaPrice.builder()
                        .setValue(PRICE))
                .build();

        PathCountProcessor processor = new PathCountProcessor();
        object.process(new RosettaPath.NullPath(), processor);

        Map<RosettaPath, Object> collectedPaths = processor.report().getCollectedPaths();
        assertEquals(1, collectedPaths.size());

        RosettaPath valuePath = RosettaPath.valueOf("resolvedPrice.value.rate");
        assertTrue(collectedPaths.containsKey(valuePath));
        assertEquals(RATE, collectedPaths.get(valuePath));
    }

    @Test
    void shouldCountValueAndExternalKey() {
        FieldWithMetaPrice object = FieldWithMetaPrice.builder()
                .setValue(PRICE)
                .setMeta(MetaFields.builder().toBuilder()
                        .setGlobalKey("global")
                        .setExternalKey("external"))
                .build();

        PathCountProcessor processor = new PathCountProcessor();
        object.process(new RosettaPath.NullPath(), processor);

        Map<RosettaPath, Object> collectedPaths = processor.report().getCollectedPaths();
        assertEquals(2, collectedPaths.size());

        RosettaPath valuePath = RosettaPath.valueOf("value.rate");
        assertTrue(collectedPaths.containsKey(valuePath));
        assertEquals(RATE, collectedPaths.get(valuePath));

        RosettaPath externalKeyPath = RosettaPath.valueOf("meta.externalKey");
        assertTrue(collectedPaths.containsKey(externalKeyPath));
        assertEquals("external", collectedPaths.get(externalKeyPath));
    }

    @Test
    void shouldCountValueAndScopedKey() {
        FieldWithMetaPrice object = FieldWithMetaPrice.builder()
                .setValue(PRICE)
                .setMeta(MetaFields.builder().toBuilder()
                        .addKey(Key.builder()
                                .setKeyValue("location")
                                .setScope("DOCUMENT")))
                .build();

        PathCountProcessor processor = new PathCountProcessor();
        object.process(new RosettaPath.NullPath(), processor);

        Map<RosettaPath, Object> collectedPaths = processor.report().getCollectedPaths();
        System.out.println(collectedPaths.keySet());
        assertEquals(2, collectedPaths.size());
        
        RosettaPath valuePath = RosettaPath.valueOf("value.rate");
        assertTrue(collectedPaths.containsKey(valuePath));
        assertEquals(RATE, collectedPaths.get(valuePath));

        RosettaPath externalKeyPath = RosettaPath.valueOf("meta.location");
        assertTrue(collectedPaths.containsKey(externalKeyPath));
        assertEquals("location", collectedPaths.get(externalKeyPath));
    }
}
