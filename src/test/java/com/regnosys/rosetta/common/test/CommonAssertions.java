package com.regnosys.rosetta.common.test;

/*-
 * #%L
 * Rosetta Common
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;

public final class CommonAssertions {

	private CommonAssertions() {
	}

	public static void assertBasicEqualsBehaviour(Object testObj) {
		assertThat("no object is equal to null", !testObj.equals(null));
		assertThat("random string check", !testObj.equals("nikos"));
		assertThat("should always be equal to itself", testObj.equals(testObj));
	}

	@SafeVarargs
	public static <T> void assertExpectedEquivalent(T refObj, T... others) {
		for (T other : others) {
			assertThat("the reference object and test objects should not be the same object", refObj, not(sameInstance(other)));
			assertThat("should be equal", refObj.equals(other));
			assertThat("equals should be symmetric", other.equals(refObj));
			assertThat("should have the same hashcode since they are equal" + other, refObj.hashCode() == other.hashCode());
		}
	}

	@SafeVarargs
	public static <T> void assertExpectedDifferent(T refObj, T... others) {
		for (T other : others) {
			assertThat("should not be equal", !refObj.equals(other));
			assertThat("should not be equal symmetrically", !other.equals(refObj));
		}
	}

	public static void assertBasicToString(Object testObj, String... featuresToFind) {
		String str = testObj.toString();
		for (String feature : featuresToFind) {
			assertThat("Expected to find " + feature + " in [" + str + "]", str.contains(feature));
		}
	}
}
