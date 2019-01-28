package com.regnosys.rosetta.common.util;

import static com.regnosys.rosetta.common.test.CommonAssertions.assertBasicEqualsBehaviour;
import static com.regnosys.rosetta.common.test.CommonAssertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;

@DisplayName("equals() and toString() tests for Pair class")
class PairTest {

	@DisplayName("basic self equals() behaviour")
	@Test
	void basic() {
		Pair<String, String> aPairOfStrings = Pair.of("left", "right");
		assertBasicEqualsBehaviour(aPairOfStrings);
		//
		Pair<Integer, Object> anotherPair = Pair.of(Integer.valueOf(2), new Object());
		assertBasicEqualsBehaviour(anotherPair);

	}

	@DisplayName("equivalency tests")
	@Test
	void equivalency() {
		Pair<String, String> one = Pair.of("left", "right");
		Pair<String, String> equal = Pair.of("left", "right");
		assertExpectedEquivalent(one, equal);
		//
		Pair<Integer, List<String>> complexPair = Pair.of(Integer.valueOf(2), ImmutableList.of("first", "second"));
		Pair<Integer, List<String>> equalComplexPair = Pair.of(Integer.valueOf(2), ImmutableList.of("first", "second"));
		assertExpectedEquivalent(complexPair, equalComplexPair);
	}

	@DisplayName("inequivalency tests")
	@Test
	void different() {
		Pair<String, String> one = Pair.of("left", "right");
		Pair<String, String> two = Pair.of("left", "otherRight");
		Pair<String, String> three = Pair.of("otherLeft", "right");
		Pair<String, String> four = Pair.of("otherLeft", "otherRight");
		assertExpectedDifferent(one, two, three, four);
		//
		Pair<Integer, Object> anotherOne = Pair.of(Integer.valueOf(2), new Object());
		Pair<Integer, Object> anotherTwo = Pair.of(Integer.valueOf(2), "String");
		Pair<Integer, Object> anotherThree = Pair.of(Integer.valueOf(4), "String");
		Pair<Integer, Object> anotherFour = Pair.of(Integer.valueOf(2), "anotherString");
		assertExpectedDifferent(anotherOne, anotherTwo, anotherThree, anotherFour);
	}

	@DisplayName("toString() tests")
	@Test
	void toStringTests() {
		assertBasicToString(Pair.of("left", "right"), "left", "right");
		assertBasicToString(Pair.of(Integer.valueOf(2), ImmutableList.of("first", "second")), "first", "second", "2");
	}

}
