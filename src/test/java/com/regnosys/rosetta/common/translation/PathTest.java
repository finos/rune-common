package com.regnosys.rosetta.common.translation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathTest {

	private static final Path TEST_PATH = Path.parse("Contract.contractualPrice.priceNotation.assetIdentifier");

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
}