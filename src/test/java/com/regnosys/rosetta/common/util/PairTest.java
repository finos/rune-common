package com.regnosys.rosetta.common.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Basic tests for Pair class")
class PairTest {

	@Test
	void test() {
		Pair<String, String> one = Pair.of("left", "right");
		Pair<String, String> equal = Pair.of("left", "right");
		Pair<String, String> unEqual = Pair.of("otherLeft", "right");
		assertThat(one, is(equal));
		assertThat(equal, is(one));
		assertThat(one, not(is(unEqual)));
		assertThat(unEqual, not(is(one)));
	}

}
