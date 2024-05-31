package com.regnosys.rosetta.common.util;

/*-
 * ==============
 * Rosetta Common
 * --------------
 * Copyright (C) 2018 - 2024 REGnosys
 * --------------
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
