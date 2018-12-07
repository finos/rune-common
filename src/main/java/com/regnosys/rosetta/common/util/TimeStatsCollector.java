package com.regnosys.rosetta.common.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashMap;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public class TimeStatsCollector {

	private static final Map<String, TimeStat> timers = new HashMap<>();
	
	public static Stopwatch startTimer(String timer) {
		TimeStat stat = timers.computeIfAbsent(timer, t->new TimeStat(t));
		Stopwatch result = new Stopwatch(stat);
		result.start();
		return result;
	}
	
	public static void resetAll() {
		timers.clear();
	}
	
	public static String statsFor(String timer) {
		return Optional.ofNullable(timers.get(timer)).map(t->t.toString()).orElse("");
	}
	public static LongSummaryStatistics rawStatsFor(String timer) {
		return Optional.ofNullable(timers.get(timer)).map(t->t.getRawStats()).orElse(null);
	}
	
	public static String allStats() {
		return timers.values().stream().map(s->s.toString()).collect(Collectors.joining("\n"));
	}
	
	public static class Stopwatch {
		private final TimeStat statCollector;		
		long startNanos;
		
		public Stopwatch(TimeStat statCollector) {
			this.statCollector = statCollector;
		}

		public void start() {
			startNanos = System.nanoTime();
		}
		
		public long stop() {
			long interval = System.nanoTime() - startNanos;
			statCollector.collect(interval);
			return interval;
		}
	}
	
	private static class TimeStat {
		private final String name;
		private final Queue<Long> times;
		
		public TimeStat(String name) {
			super();
			this.name = name;
			times = new ConcurrentLinkedQueue<>();
		}

		public void collect(long interval) {
			times.add(interval);
		}
		
		public String toString() {
			LongSummaryStatistics stats = getRawStats();
			return name +" - Total:"+toTimeString(stats.getSum())+" count:"+stats.getCount()+
					" average:"+toTimeString(stats.getAverage())+" min:"+toTimeString(stats.getMin()) + 
					" max:"+toTimeString(stats.getMax());
		}

		public LongSummaryStatistics getRawStats() {
			LongSummaryStatistics stats = times.stream().collect(Collectors.summarizingLong(l->l));
			return stats;
		}
		
		private String toTimeString(long t) {
			BigDecimal dVal = new BigDecimal(t);
			return toTimeString(dVal);
		}
		private String toTimeString(double t) {
			BigDecimal dVal = new BigDecimal(t);
			return toTimeString(dVal);
		}

		private String[] suffixes = new String[]{"ns", "\u03bcs", "ms", "s", "ks", "Ms", "Gs"};
		private final static BigDecimal THOUSAND = new BigDecimal(1000);
		private String toTimeString(BigDecimal dVal) {
			int divisions = 0;
			while (dVal.compareTo(THOUSAND)>0) {
				dVal = dVal.divide(THOUSAND);
				divisions++;
			}
			return dVal.round(new MathContext(3))+ suffixes[divisions];
		}
	}
}
