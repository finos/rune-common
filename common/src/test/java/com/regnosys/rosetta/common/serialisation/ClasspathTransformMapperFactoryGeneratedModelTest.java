package com.regnosys.rosetta.common.serialisation;

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rosetta.model.lib.annotations.RuneLabelProvider;
import com.rosetta.model.lib.functions.LabelProvider;
import com.rosetta.model.lib.functions.RosettaFunction;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.records.Date;
import com.rosetta.model.lib.transform.Ingest;
import com.rosetta.model.lib.transform.SerializationFormat;
import csv.test.labelled.LabelledTrade;
import csv.test.user.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * End to end against a DSL-<b>generated</b> model, as opposed to the hand-written stub interfaces
 * {@link ClasspathTransformMapperFactoryTest} uses. Proves the two things a stub cannot: that the Rune
 * code generator actually stamps {@code @RuneLabelProvider} on a labelled type's pojo interface (rather
 * than the mechanism merely being correct if it did), and that an ingest's function-rooted provider is
 * really overridden by a generated type-rooted one when both exist.
 *
 * <p>Needs {@code rune.dsl.version} 10.7.0 or later, which carries finos/rune-dsl#1358.
 */
class ClasspathTransformMapperFactoryGeneratedModelTest {

    private final ClasspathTransformMapperFactory factory = new ClasspathTransformMapperFactory();

    private static LabelledTrade buildTrade() {
        return LabelledTrade.builder()
                .setTradeId("T-001")
                .setNotionalAmount(BigDecimal.valueOf(1000000))
                .setQuantity(10)
                .setTradeDate(Date.of(2026, 1, 15))
                .setIsCleared(true)
                .build();
    }

    /**
     * The CSV-import case this task exists for: an ingest whose function-rooted provider is (wrongly)
     * rooted at some unrelated output type — modelled here by {@link User}, standing in for "any type
     * that isn't the CSV input" — rather than the CSV input, {@link LabelledTrade}. Before this task that
     * wrongly rooted provider is all the resolver could ever find; passing {@link LabelledTrade} as the
     * {@link TransformRoot}'s type must now win instead, sourced from the annotation the generator
     * actually stamped.
     */
    public static class UnrelatedOutputLabelProvider implements LabelProvider {
        @Override
        public String getLabel(RosettaPath path) {
            return "wrong:" + path.buildPath();
        }
    }

    @Ingest(format = SerializationFormat.CSV_LABELLED)
    @RuneLabelProvider(labelProvider = UnrelatedOutputLabelProvider.class)
    private static class IngestLabelledTradeWithUnrelatedFunctionProvider implements RosettaFunction {
    }

    @Test
    void generatedTypeRootedLabelProviderIsFoundAndRoundTrips() throws JsonProcessingException {
        // No function at all: proves the generated annotation on LabelledTrade is found and used purely
        // from rootType, the shape a CSV importer with no transform function needs.
        ObjectMapper mapper = factory.create(
                new TransformSerialization(SerializationFormat.CSV_LABELLED, null), null,
                TransformRoot.input(LabelledTrade.class));

        LabelledTrade original = buildTrade();
        String csv = mapper.writeValueAsString(original);
        String header = csv.substring(0, csv.indexOf('\n'));

        assertEquals("\"Trade ID\",\"Notional Amount\",Quantity,\"Trade Date\",\"Is Cleared\"", header);
        assertEquals(original, mapper.readValue(csv, LabelledTrade.class));
    }

    @Test
    void ingestBindsGeneratedLabelledInputTypeByLabelDespiteAnUnrelatedFunctionProvider() throws JsonProcessingException {
        TransformSerialization s =
                TransformSerializationResolver.input(IngestLabelledTradeWithUnrelatedFunctionProvider.class).get();
        ObjectMapper mapper =
                factory.create(s, IngestLabelledTradeWithUnrelatedFunctionProvider.class,
                        TransformRoot.input(LabelledTrade.class));

        // Header columns deliberately reordered from the write order, so a pass here proves the reader
        // binds by label text (via the generated LabelledTradeLabelProvider) rather than by position.
        String csv = "\"Trade ID\",Quantity,\"Notional Amount\",\"Is Cleared\",\"Trade Date\"\n"
                + "T-001,10,1000000,true,2026-01-15\n";

        assertEquals(buildTrade(), mapper.readValue(csv, LabelledTrade.class));
    }
}
