package com.regnosys.rosetta.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LongSummaryStatistics;

import org.junit.jupiter.api.Test;

import com.regnosys.rosetta.common.util.TimeStatsCollector.Stopwatch;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;


class TimeStatsTest {

	@Test
	void test() throws InterruptedException {
		Stopwatch startTimer = TimeStatsCollector.startTimer("timer");
		Thread.sleep(50);
		startTimer.stop();
		startTimer = TimeStatsCollector.startTimer("timer");
		Thread.sleep(50);
		startTimer.stop();
		LongSummaryStatistics stats=TimeStatsCollector.rawStatsFor("timer");
		assertEquals(2, stats.getCount());
		assertThat((double)stats.getSum(), closeTo(100000000.0, 20000000.0));
		assertThat(stats.getAverage(), closeTo(50000000.0, 10000000.0));
		System.out.println(TimeStatsCollector.statsFor("timer"));
	}

}
