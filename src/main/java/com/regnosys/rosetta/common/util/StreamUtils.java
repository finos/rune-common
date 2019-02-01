package com.regnosys.rosetta.common.util;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public class StreamUtils {
	public static <A> Function<A, Stream<A>> flattenTreeC(Function<A, Collection<A>> extract) {
		Function<A, Stream<A>> streamExtract = extract.andThen(as -> as.stream());
		return flattenTree(streamExtract, new HashSet<>());
	}

	public static <A> Function<A, Stream<A>> flattenTree(Function<A, Stream<A>> extract) {
		return flattenTree(extract, new HashSet<>());
	}

	public static <A> Stream<A> flattenTreeC(A initial, Function<A, Collection<A>> extract) {
		return Stream.of(initial)
					 .flatMap(a -> Stream.concat(Stream.of(a), extract.apply(a).stream()));
	}

	public static <A> Function<A, Stream<A>> flattenTree(Function<A, Stream<A>> extract, Collection<A> visited) {
		return a -> {
			if (visited.contains(a))
				return Stream.empty();
			visited.add(a);
			return Stream.concat(Stream.of(a), extract.apply(a).flatMap(StreamUtils.flattenTree(extract)));
		};
	}

	public static <A> void visitTreeC(A initial, Consumer<A> visitFunc, Function<A, Collection<A>> traverseFunc) {
		Deque<A> toVisit = new ArrayDeque<>();
		Set<A> visited = new HashSet<>();
		toVisit.add(initial);
		while (!toVisit.isEmpty()) {
			A a = toVisit.removeFirst();
			if (visited.contains(a))
				continue;
			visitFunc.accept(a);
			Collection<A> nexts = traverseFunc.apply(a);
			toVisit.addAll(nexts);
			visited.add(a);
		}
	}

	public static <A> void visitBiTree(A initial, Consumer<A> visitFunction,
			Function<A, Collection<A>> traverseFunc1,
			Function<A, Collection<A>> traverseFunc2) {
		Deque<A> toVisit = new ArrayDeque<>();
		Set<A> visited = new HashSet<>();
		toVisit.add(initial);
		while (!toVisit.isEmpty()) {
			A a = toVisit.removeFirst();
			if (visited.contains(a))
				continue;
			visitFunction.accept(a);
			Collection<A> nexts = traverseFunc1.apply(a);
			toVisit.addAll(nexts);
			nexts = traverseFunc2.apply(a);
			toVisit.addAll(nexts);
			visited.add(a);
		}
	}

	public static <A> void visitTreeS(A initial, Consumer<A> visitFunc, Function<A, Stream<A>> traversFunc) {
		Deque<A> toVisit = new ArrayDeque<>();
		Set<A> visited = new HashSet<>();
		toVisit.add(initial);
		while (!toVisit.isEmpty()) {
			A a = toVisit.removeFirst();
			if (visited.contains(a))
				continue;
			visited.add(a);
			visitFunc.accept(a);
			traversFunc.apply(a).forEach(toVisit::add);
		}
	}

	public static <A> Stream<A> recurse(A a, Function<A, A> func) {
		return recurse(a, func, new HashSet<>());
	}

	public static <A> Stream<A> recurse(A a, Function<A, A> func, Collection<A> visited) {
		if (a == null)
			return Stream.empty();
		if (visited.contains(a))
			return Stream.empty();
		visited.add(a);
		return Stream.concat(Stream.of(a), recurse(func.apply(a), func, visited));
	}

	public static <A> Stream<A> optionalStream(Collection<A> c) {
		if (c == null)
			return Stream.empty();
		return c.stream();
	}

	public static <T> Predicate<T> distinctByKey(
			Function<? super T, ?> ke) {
		Map<Object, Boolean> seen = new ConcurrentHashMap<>();
		return t -> seen.putIfAbsent(ke.apply(t), Boolean.TRUE) == null;
	}

	public static <T> UnaryOperator<T> peek(Consumer<T> c) {
		return x -> {
			c.accept(x);
			return x;
		};
	}
}
