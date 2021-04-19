package com.regnosys.rosetta.common.translation.flat;

import org.junit.jupiter.api.Test;

import static com.regnosys.rosetta.common.translation.flat.IndexCapturePath.IndexCapturePathElement;
import static org.junit.jupiter.api.Assertions.assertEquals;

class IndexCapturePathTest {

	@Test
	void test() {
		assertEquals("activity", IndexCapturePathElement.parse("activity").toString());
		assertEquals("activity[1]", IndexCapturePathElement.parse("activity[1]").toString());
		assertEquals("activity[activityNum]",IndexCapturePathElement.parse("activity[activityNum]").toString());
		assertEquals("activity[1]",IndexCapturePathElement.parse("activity(1)").toString());
		assertEquals("activity[activityNum]",IndexCapturePathElement.parse("activity(activityNum)").toString());
	}

}
