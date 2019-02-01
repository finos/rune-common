package com.regnosys.rosetta.common.util;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.collect.Iterables.*;
import static java.lang.String.format;

public class HierarchicalPath {
	private final HierarchicalPath parent;
	private final HierarchicalPath.Element element;

	private HierarchicalPath(HierarchicalPath parent, Element element) {
		this.parent = parent;
		this.element = element;
	}

	public static HierarchicalPath createPath(HierarchicalPath parent, HierarchicalPath.Element element) {
		return new HierarchicalPath(parent, element);
	}

	public static HierarchicalPath createPath(HierarchicalPath.Element element) {
		return new HierarchicalPath(null, element);
	}

	public static HierarchicalPath createPathFromElements(List<Element> elements) {
		HierarchicalPath newPath = null;

		for (Element element : elements) {
			if (newPath == null) {
				newPath = createPath(Element.create(element.uri, element.path, element.index, element.attrs));
			} else {
				newPath = newPath.newSubPath(element);
			}
		}
		return newPath;
	}

	public static HierarchicalPath valueOf(String stringPath) {
		Iterable<String> pathSections = Splitter.on('.').split(stringPath);
		if (size(pathSections) == 0) {
			throw new IllegalArgumentException(stringPath + " is not a valid rosetta path");
		}

		HierarchicalPath path = HierarchicalPath.createPath(Element.valueOf(getFirst(pathSections, null)));
		for (String section : skip(pathSections, 1)) {
			path = path.newSubPath(Element.valueOf(section));
		}
		return path;
	}

	public HierarchicalPath newSubPath(Element element) {
		if (parent == null && element == null) {
			return new HierarchicalPath(null, null);
		} else {
			return new HierarchicalPath(this, element);
		}
	}

	public HierarchicalPath trimFirst() {
		LinkedList<Element> elements = allElements();
		elements.removeFirst();
		return createPathFromElements(elements);
	}

	public LinkedList<Element> allElements() {
		LinkedList<Element> elements = new LinkedList<>();
		if (hasParent()) {
			elements.addAll(parent.allElements());
		}
		elements.add(element);

		return elements;
	}

	public List<String> allElementPaths() {
		return allElements().stream()
							.map(HierarchicalPath.Element::getPath)
							.collect(Collectors.toList());
	}

	public String buildPath() {
		if (hasParent()) {
			return getParent().buildPath() + "." + element.asPathString();
		}
		return element.asPathString();
	}

	public HierarchicalPath getParent() {
		return parent;
	}

	public Element getElement() {
		return element;
	}

	private boolean hasParent() {
		return !Objects.isNull(parent);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		HierarchicalPath that = (HierarchicalPath) o;

		if (parent != null ? !parent.equals(that.parent) : that.parent != null)
			return false;
		return element != null ? element.equals(that.element) : that.element == null;
	}

	@Override
	public String toString() {
		return buildPath();
	}

	@Override
	public int hashCode() {
		int result = parent != null ? parent.hashCode() : 0;
		result = 31 * result + (element != null ? element.hashCode() : 0);
		return result;
	}

	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	public static class Element {
		public static final String DEFAULT_URI = "FpML_5_10";

		private final String uri;
		private final String path;
		private final OptionalInt index;
		private final Map<String, String> attrs;

		private Element(String uri, String path, OptionalInt index, Map<String, String> attrs) {
			this.uri = uri;
			this.path = path;
			this.index = index;
			this.attrs = attrs == null ? Collections.emptyMap() : attrs;
		}

		public static Element create(String path, Map<String, String> attrs) {
			return new Element(DEFAULT_URI, path, OptionalInt.empty(), attrs);
		}

		public static Element create(String path, OptionalInt index, Map<String, String> attrs) {
			return new Element(DEFAULT_URI, path, index, attrs);
		}

		public static Element create(String uri, String path, OptionalInt index, Map<String, String> attrs) {
			return new Element(uri, path, index, attrs);
		}

		/**
		 * @param element of the form: fieldName(index)[attributes]=value, where (index) and [attributes] are optional
		 */
		public static Element valueOf(String element) {
			Pattern p = Pattern.compile("^(\\w+)(?:\\(([0-9]+)\\))?(?:\\[([\\w\\-]+)=([\\w\\-]+)])?$");
			Matcher matcher = p.matcher(element);

			if (!matcher.matches()) {
				throw new IllegalArgumentException(element + " is not a valid path element");
			}
			OptionalInt index = matcher.group(2) == null ? OptionalInt.empty() : OptionalInt.of(Integer.parseInt(matcher.group(2)));
			String attrKey = matcher.group(3);
			String attrValue = matcher.group(4);
			ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
			if (attrKey != null && attrValue != null) {
				builder.put(attrKey, attrValue);
			}
			return create(matcher.group(1), index, builder.build());
		}

		public String getUri() {
			return uri;
		}

		public String getPath() {
			return path;
		}

		public OptionalInt getIndex() {
			return index;
		}

		private String asPathString() {
			String idSection = !attrs.isEmpty() ?
					attrs.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(",", "[", "]")) :
					"";
			String indexSection = index.isPresent() ? format("(%s)", index.getAsInt()) : "";
			return format("%s%s%s", path, indexSection, idSection);
		}

		@Override
		public String toString() {
			return "Element{" +
					"path='" + path + '\'' +
					", index=" + index +
					", uri='" + uri + '\'' +
					", attrs=" + attrs +
					'}';
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;

			Element element = (Element) o;

			if (uri != null ? !uri.equals(element.uri) : element.uri != null)
				return false;
			if (path != null ? !path.equals(element.path) : element.path != null)
				return false;
			if (index != null ? !index.equals(element.index) : element.index != null)
				return false;
			return attrs != null ? attrs.equals(element.attrs) : element.attrs == null;
		}

		@Override
		public int hashCode() {
			int result = uri != null ? uri.hashCode() : 0;
			result = 31 * result + (path != null ? path.hashCode() : 0);
			result = 31 * result + (index != null ? index.hashCode() : 0);
			result = 31 * result + (attrs != null ? attrs.hashCode() : 0);
			return result;
		}
	}

}
