package com.regnosys.rosetta.common.translation;

/*-
 * #%L
 * Rune Common
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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PathTest {

	private static final Path TEST_PATH = Path.parse("Contract.contractualPrice.priceNotation.assetIdentifier");
	private static final Path TEST_PATH2 = Path.parse("answers.partyA.access_conditions.assetIdentifier.additional_termination_event[0].name");

	@Test
	void shouldMatchEndsWithFullPath() {
		assertTrue(TEST_PATH.endsWith("Contract.contractualPrice.priceNotation.assetIdentifier".split("\\.")));
	}

	@Test
	void shouldMatchEndsWithPartialPath() {
		assertTrue(TEST_PATH.endsWith("PriceNotation.assetIdentifier".split("\\.")));
	}

	@Test
	void shouldMatchEndsWithLastElement() {
		assertTrue(TEST_PATH.endsWith("AssetIdentifier".split("\\.")));
	}

	@Test
	void shouldNotMatchEndsWithMissingLastElement() {
		assertFalse(TEST_PATH.endsWith("Contract.contractualPrice.priceNotation".split("\\.")));
	}

	@Test
	void shouldMatchNameWithoutWildcard() {
		assertTrue(TEST_PATH.nameStartMatches(TEST_PATH));
		assertTrue(TEST_PATH.nameStartMatches(TEST_PATH, false));
		assertTrue(TEST_PATH.nameStartMatches(TEST_PATH, true));
	}

	@Test
	void shouldMatchNameWithWildcard() {
		Path other = Path.parse("Contract.contractualPrice.*.assetIdentifier", true);
		assertTrue(TEST_PATH.nameStartMatches(other, true));
	}

	@Test
	void shouldMatchNameWithWildcard2() {
		Path p1 = Path.parse("*.trade.swap.swapStream.paymentDates.calculationPeriodDatesReference", true);
		Path p2 = Path.parse("dataDocument.trade.swap.swapStream.paymentDates.calculationPeriodDatesReference");
		assertTrue(p1.nameStartMatches(p2, true));
	}

	@Test
	void shouldNotMatchNameWithWildcard() {
		Path other = Path.parse("Contract.contractualPrice.*.assetIdentifier", true);
		assertFalse(TEST_PATH.nameStartMatches(other, false));
	}

	@Test
	void shouldFullMatch() {
		assertTrue(TEST_PATH2.fullStartMatches(TEST_PATH2));
	}

	@Test
	void shouldNotFullMatchBecauseDifferentIndex() {
		Path other = Path.parse("answers.partyA.access_conditions.assetIdentifier.additional_termination_event[1].name");
		assertFalse(TEST_PATH2.fullStartMatches(other));
	}

	@Test
	void shouldFullMatchNoIndex() {
		Path other = Path.parse("answers.partyA.access_conditions.assetIdentifier.additional_termination_event.name");
		assertTrue(TEST_PATH2.fullStartMatches(other));
	}

	@Test
	void shouldFullStartMatchWildcard() {
		Path subPath = Path.parse("partyA.access_conditions").prefixWithWildcard();
		assertTrue(subPath.fullStartMatches(TEST_PATH2, true));
	}

	@Test
	void shouldFullMatchWildcardNoAllowed() {
		Path subPath = Path.parse("partyA.access_conditions").prefixWithWildcard();
		assertFalse(subPath.fullStartMatches(TEST_PATH2, false));
	}

	@Test
	void shouldNotFullStartMatchWildcard() {
		Path subPath = Path.parse("partyA.access_conditions.no_match").prefixWithWildcard();
		assertFalse(subPath.fullStartMatches(TEST_PATH2, true));
	}

	@Test
	void shouldNotNameMatchOnEmptyPath() {
		assertFalse(new Path().nameStartMatches(TEST_PATH));
		assertFalse(TEST_PATH.nameStartMatches(new Path()));
	}

	@Test
	void shouldNameMatchOnBothEmptyPaths() {
		assertTrue(new Path().nameStartMatches(new Path()));
	}

	@Test
	void shouldNotFullMatchOnEmptyPath() {
		assertFalse(new Path().fullStartMatches(TEST_PATH));
		assertFalse(TEST_PATH.fullStartMatches(new Path()));
	}

	@Test
	void shouldFullMatchOnBothEmptyPaths() {
		assertTrue(new Path().fullStartMatches(new Path()));
	}
}
