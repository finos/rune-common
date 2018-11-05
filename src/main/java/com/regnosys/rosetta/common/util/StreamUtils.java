package com.regnosys.rosetta.common.util;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public class StreamUtils {
	public static <A>  Function<A, Stream<A>> flattenTreeC(Function<A, Collection<A>> extract) {
		Function<A, Stream<A>> streamExtract = extract.andThen(as->as.stream());
		return flattenTree(streamExtract, new HashSet<>());
	}
	
	public static <A>  Function<A, Stream<A>> flattenTree(Function<A, Stream<A>> extract) {
		return flattenTree(extract, new HashSet<>());
	}
	
	public static <A>  Function<A, Stream<A>> flattenTree(Function<A, Stream<A>> extract, Collection<A> visited) {
		return a-> {
			if (visited.contains(a)) return Stream.empty();
			visited.add(a);
			return Stream.concat(Stream.of(a), extract.apply(a).flatMap(StreamUtils.flattenTree(extract)));
		};
	}
	
	public static <A> void visitTreeC(A initial, Consumer<A> visitFunc, Function<A, Collection<A>> traversFunc) {
		Deque<A> toVisit = new ArrayDeque<>();
		Set<A> visited = new HashSet<>();
		toVisit.add(initial);
		while (toVisit.isEmpty()) {
			A a = toVisit.removeFirst();
			if (visited.contains(a)) continue;
			visitFunc.accept(a);
			Collection<A> nexts = traversFunc.apply(a);
			toVisit.addAll(nexts);
		}
	}
	
	public static <A> void visitTreeS(A initial, Consumer<A> visitFunc, Function<A, Stream<A>> traversFunc) {
		Deque<A> toVisit = new ArrayDeque<>();
		Set<A> visited = new HashSet<>();
		toVisit.add(initial);
		while (!toVisit.isEmpty()) {
			A a = toVisit.removeFirst();
			if (visited.contains(a)) continue;
			visited.add(a);
			visitFunc.accept(a);
			traversFunc.apply(a).forEach(toVisit::add);
		}
	}
	
	public static <A> Stream<A> recurse(A a, Function<A, A> func) {
		return recurse(a, func, new HashSet<>());
	}
	
	public static <A> Stream<A> recurse(A a, Function<A, A> func, Collection<A> visited) {
		if (a==null) return Stream.empty();
		if (visited.contains(a)) return Stream.empty();
		visited.add(a);
		return Stream.concat(Stream.of(a), recurse(func.apply(a), func, visited));
	}
	
	public static <T> UnaryOperator<T> peek(Consumer<T> c) {
	    return x -> {
	        c.accept(x);
	        return x;
	    };
	}
}
