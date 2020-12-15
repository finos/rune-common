package com.regnosys.rosetta.common.translation;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

class MappingProcessorStepTest {

    private static final Foo FOO_1 = new Foo("A.b.c.contract.tradableProduct");
    private static final Bar BAR_1 = new Bar("A.b(1).c.contract.tradableProduct.product.contractualProduct.economicTerms.payout.creditDefaultPayout.generalTerms.buyerSeller.buyer");
    private static final Bar BAR_2 = new Bar("A.b(1).c.contract.tradableProduct.product.contractualProduct.economicTerms.payout.creditDefaultPayout.generalTerms.buyerSeller.seller");
    private static final Bar BAR_3 = new Bar("A.b(1).c.contract.tradableProduct.product.contractualProduct.economicTerms.payout.interestRatePayout(0).payerReceiver.payer");
    private static final Bar BAR_4 = new Bar("A.b(1).c.contract.tradableProduct.product.contractualProduct.economicTerms.payout.interestRatePayout(0).payerReceiver.receiver");
    private static final Bar BAR_5 = new Bar("A.b(1).c.contract.tradableProduct.product.contractualProduct.economicTerms.payout.cashflow.payerReceiver.payer");
    private static final Bar BAR_6 = new Bar("A.b(1).c.contract.tradableProduct.product.contractualProduct.economicTerms.payout.cashflow.payerReceiver.receiver");
    private static final Foo FOO_2 = new Foo("A.b(2).c.contract.tradableProduct");
    private static final Bar BAR_7 = new Bar("A.b(2).c.contract.tradableProduct.product.contractualProduct.economicTerms.payout.creditDefaultPayout.generalTerms.buyerSeller.buyer");
    private static final Bar BAR_8 = new Bar("A.b(2).c.contract.tradableProduct.product.contractualProduct.economicTerms.payout.creditDefaultPayout.generalTerms.buyerSeller.seller");
    private static final Bar BAR_9 = new Bar("A.b(2).c.contract.tradableProduct.product.contractualProduct.economicTerms.payout.interestRatePayout(0).payerReceiver.payer");
    private static final Bar BAR_10 = new Bar("A.b(2).c.contract.tradableProduct.product.contractualProduct.economicTerms.payout.interestRatePayout(0).payerReceiver.receiver");
    private static final Bar BAR_11 = new Bar("A.b(2).c.contract.tradableProduct.product.contractualProduct.economicTerms.payout.cashflow.payerReceiver.payer");
    private static final Bar BAR_12 = new Bar("A.b(2).c.contract.tradableProduct.product.contractualProduct.economicTerms.payout.cashflow.payerReceiver.receiver");

    // Mappers in randomised order
    private static final List<MappingDelegate> MAPPERS =
            Arrays.asList(BAR_10, BAR_3, BAR_1, BAR_11, BAR_4, BAR_12, BAR_6, FOO_2, BAR_7, FOO_1, BAR_2, BAR_9, BAR_8, BAR_5);

    @Test
    void shouldSortByPathWithCashflowPayoutLast() {
        List<MappingDelegate> mappingDelegates = new ArrayList<>(MAPPERS);
        mappingDelegates.sort(MappingProcessorStep.MAPPING_DELEGATE_COMPARATOR);
        // assert list order
        assertThat(mappingDelegates,
                contains(FOO_1, BAR_1, BAR_2, BAR_3, BAR_4, BAR_5, BAR_6, FOO_2, BAR_7, BAR_8, BAR_9, BAR_10, BAR_11, BAR_12));
    }

    @Test
    void shouldCompleteWithinExpectedTimeout() throws InterruptedException {
        ThreadPoolExecutor executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        MappingContext mappingContext = new MappingContext(Lists.newArrayList(), Maps.newHashMap(), executorService);

        CompletableFuture<Object> completableFuture = CompletableFuture.completedFuture(null);
        mappingContext.getInvokedTasks().add(completableFuture);

        MappingProcessorStep mappingProcessorStep = new MappingProcessorStep(Lists.newArrayList(), mappingContext, 10);

        mappingProcessorStep.runProcessStep(TestModel.class, new TestModelBuilder());

        Thread.sleep(20);
        assertThat(executorService.getActiveCount(), equalTo(0));
        assertThat(mappingContext.getMappingErrors().isEmpty(), equalTo(true));
        executorService.shutdown();
    }

    @Test
    void shouldTerminateUncompletedInvokedTasks() throws InterruptedException {
        ThreadPoolExecutor executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        MappingContext mappingContext = new MappingContext(Lists.newArrayList(), Maps.newHashMap(), executorService);
        mappingContext.getInvokedTasks().add(new CompletableFuture<>());

        MappingProcessorStep mappingProcessorStep = new MappingProcessorStep(Lists.newArrayList(), mappingContext, 10);

        mappingProcessorStep.runProcessStep(TestModel.class, new TestModelBuilder());

        Thread.sleep(20);
        assertThat(executorService.getActiveCount(), equalTo(0));
        assertThat(mappingContext.getMappingErrors(), contains("Timeout running mapping processors"));
        executorService.shutdown();
    }

    @Test
    void shouldLogErrorFromInvokedTask() throws InterruptedException {
        ThreadPoolExecutor executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        MappingContext mappingContext = new MappingContext(Lists.newArrayList(), Maps.newHashMap(), executorService);

        CompletableFuture<Object> completableFuture = CompletableFuture.supplyAsync(() -> {
            throw new RuntimeException("Error running task");
        });
        mappingContext.getInvokedTasks().add(completableFuture);

        MappingProcessorStep mappingProcessorStep = new MappingProcessorStep(Lists.newArrayList(), mappingContext, 50);

        mappingProcessorStep.runProcessStep(TestModel.class, new TestModelBuilder());

        Thread.sleep(100);
        assertThat(executorService.getActiveCount(), equalTo(0));
        assertThat(mappingContext.getMappingErrors(), contains("Error running mapping processors: java.lang.RuntimeException: Error running task"));
        executorService.shutdown();
    }

    @Test
    void shouldLogErrorWhenThereIsMappingProcessorError() throws InterruptedException {
        ThreadPoolExecutor executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        MappingContext mappingContext = new MappingContext(Lists.newArrayList(), Maps.newHashMap(), executorService);

        CompletableFuture<Object> completableFuture = CompletableFuture.completedFuture(null);
        mappingContext.getInvokedTasks().add(completableFuture);

        MappingProcessorStep mappingProcessorStep = new MappingProcessorStep(Lists.newArrayList(FOO_1), mappingContext, 10);

        mappingProcessorStep.runProcessStep(TestModel.class, new BrokenTestModelBuilder());

        Thread.sleep(20);
        assertThat(executorService.getActiveCount(), equalTo(0));
        assertThat(mappingContext.getMappingErrors(), contains("Error running mapping processors: java.lang.RuntimeException: Builder process error"));
        executorService.shutdown();
    }

    private static class Foo extends MappingProcessor {
        public Foo(String modelPath) {
            super(RosettaPath.valueOf(modelPath), Collections.emptyList(), null);
        }

        @Override
        public String toString() {
            return "Foo{" + getModelPath().buildPath() + "}";
        }
    }

    private static class Bar extends MappingProcessor {
        public Bar(String modelPath) {
            super(RosettaPath.valueOf(modelPath), Collections.emptyList(), null);
        }

        @Override
        public String toString() {
            return "Bar{" + getModelPath().buildPath() + "}";
        }
    }

    static class TestModel extends RosettaModelObject {
        private final String value;

        public TestModel(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public RosettaModelObjectBuilder toBuilder() {
            return new TestModelBuilder();
        }

        @Override
        protected void process(RosettaPath path, Processor processor) {
            throw new UnsupportedOperationException();
        }

        @Override
        public RosettaMetaData<? extends RosettaModelObject> metaData() {
            return null;
        }
    }

    static class TestModelBuilder extends RosettaModelObjectBuilder {
        protected String value;

        @Override
        public RosettaModelObject build() {
            return new TestModel(value);
        }

        @Override
        public <B extends RosettaModelObjectBuilder> B prune() {
            return null;
        }

        @Override
        public boolean hasData() {
            return false;
        }

        @Override
        public RosettaMetaData<? extends RosettaModelObject> metaData() {
            return null;
        }

        @Override
        public void process(RosettaPath rosettaPath, BuilderProcessor builderProcessor) {

        }

        @Override
        public <B extends RosettaModelObjectBuilder> B merge(B b, BuilderMerger builderMerger) {
            return null;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    static class BrokenTestModelBuilder extends  TestModelBuilder {
        public BrokenTestModelBuilder() {
            super();
        }

        @Override
        public void process(RosettaPath rosettaPath, BuilderProcessor builderProcessor) {
           throw new RuntimeException("Builder process error");
        }
    }
}