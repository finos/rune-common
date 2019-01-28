package com.regnosys.rosetta.common.test;

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
