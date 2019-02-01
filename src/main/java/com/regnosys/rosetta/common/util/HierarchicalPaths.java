package com.regnosys.rosetta.common.util;

import com.google.common.collect.ImmutableList;

import java.util.Iterator;
import java.util.stream.Stream;

public class HierarchicalPaths implements Iterable<HierarchicalPath> {
	private final ImmutableList<HierarchicalPath> paths;

	public HierarchicalPaths(ImmutableList<HierarchicalPath> paths) {
		this.paths = paths;
	}

	@Override
	public Iterator<HierarchicalPath> iterator() {
		return paths.iterator();
	}

	public Stream<HierarchicalPath> stream() {
		return paths.stream();
	}
}
