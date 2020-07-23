package com.regnosys.rosetta.common.translation;

import com.rosetta.model.lib.path.RosettaPath;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

class MappingProcessorStepTest {

	private static final Foo FOO_1 = new Foo("A.b(1).c.contract.tradableProduct");
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
}