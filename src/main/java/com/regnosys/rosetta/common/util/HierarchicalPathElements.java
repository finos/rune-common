package com.regnosys.rosetta.common.util;

import com.google.common.collect.ImmutableList;

import java.util.Iterator;
import java.util.List;

public class HierarchicalPathElements implements Iterable<HierarchicalPath.Element> {
	private final List<HierarchicalPath.Element> elements;

	private HierarchicalPathElements(List<HierarchicalPath.Element> elements) {
		this.elements = elements;
	}

	@Override
	public Iterator<HierarchicalPath.Element> iterator() {
		return elements.iterator();
	}

	public static HierarchicalPathElementsBuilder builder() {
		return new HierarchicalPathElementsBuilder();
	}

	public static class HierarchicalPathElementsBuilder {
		private final ImmutableList.Builder<HierarchicalPath.Element> elements;

		public HierarchicalPathElementsBuilder() {
			elements = ImmutableList.builder();
		}

		public HierarchicalPathElementsBuilder add(HierarchicalPath.Element element) {
			elements.add(element);
			return this;
		}

		public HierarchicalPathElements build() {
			return new HierarchicalPathElements(elements.build());
		}
	}
}
