package com.regnosys.rosetta.common.translation.flat;

/*-
 * ==============
 * Rosetta Common
 * ==============
 * Copyright (C) 2018 - 2024 REGnosys
 * ==============
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
 * ==============
 */

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class IndexCapturePath implements Comparable<IndexCapturePath> {

	private final IndexCapturePathElement[] elements; 
	
	public static IndexCapturePath parse(String s) {
		String[] splits = s.split("\\.");
		IndexCapturePathElement[] elements = Arrays.stream(splits).map(e->IndexCapturePathElement.parse(e)).toArray(i->new IndexCapturePathElement[i]);
		return new IndexCapturePath(elements);
	}

	private IndexCapturePath(IndexCapturePathElement[] elements) {
		this.elements = elements;
	}
	
	public IndexCapturePath toUnindexed() {
		return new IndexCapturePath(Arrays.stream(elements).map(e->e.toUnindexed()).toArray(i->new IndexCapturePathElement[i]));
	}
	
	public Map<String, Integer> captureIndexes(IndexCapturePath indexed) {
		if (!this.matches(indexed)) throw new RuntimeException("Paths must match");
		Map<String, Integer> result = new HashMap<>();
		for (int i=0;i<elements.length;i++) {
			elements[i].captureIdex(result, indexed.elements[i]);
		}
		return result;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(elements);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IndexCapturePath other = (IndexCapturePath) obj;
		if (!Arrays.equals(elements, other.elements))
			return false;
		return true;
	}
	
	public boolean matches(IndexCapturePath other) {
		if (this == other)
			return true;
		if (other == null)
			return false;
		if (elements.length!=other.elements.length)
			return false;
		for (int i=0;i<elements.length;i++) {
			if (!elements[i].matches(other.elements[i])) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int compareTo(IndexCapturePath o) {
		for (int i=0;i<Math.min(this.elements.length, o.elements.length);i++) {
			int elCompare = elements[i].compareTo(o.elements[i]);
			if (elCompare!=0) return elCompare;
		}
		int lengthDif = elements.length - o.elements.length;
		return lengthDif;
	}

	@Override
	public String toString() {
		return "IndexCapturePath [" + Arrays.stream(elements).map(e->e.toString()).collect(Collectors.joining(".")) + "]";
	}

	public static class IndexCapturePathElement implements Comparable<IndexCapturePathElement>{
		private final String elementName;
		private final Optional<String> indexCaptureName;
		private final Optional<Integer> capturedIndex;
		
		//this pattern matches an element name (containing any characters except [ and (
		//followed by an optional index term consisting of a bracket ( or [
		//then either an index label (java indentifier rules) or an index number
		//e.g activity or activity[1] or activity[activityNum] or activity(1) or activity(activityNum) 
		private static final Pattern elementPattern = Pattern.compile("(?<name>[^\\[(]*)((\\(|\\[)((?<capture>[A-Za-z_]+[A-Za-z0-9_]*)|(?<index>[0-9]+))(\\)|\\]))?");
		
		public static IndexCapturePathElement parse(String s) {
			Matcher m= elementPattern.matcher(s);
			m.matches();
			String name = m.group("name");
			String capture = m.group("capture");

			if ("".equals(capture)) capture=null;
			String index = m.group("index");
			if ("".equals(index)) index=null;
			Optional<Integer> indexOp = Optional.ofNullable(index).map(Integer::parseInt);
			return new IndexCapturePathElement(name, Optional.ofNullable(capture), indexOp);
		}
		
		public void captureIdex(Map<String, Integer> result, IndexCapturePathElement other) {
			if (indexCaptureName.isPresent() && other.capturedIndex.isPresent()) {
				result.put(indexCaptureName.get(), other.capturedIndex.get());
			}
		}
		
		public IndexCapturePathElement toUnindexed() {
			return new IndexCapturePathElement(elementName, Optional.empty(), Optional.empty());
		}

		private IndexCapturePathElement(String elementName, Optional<String> indexCaptureName, Optional<Integer> capturedIndex) {
			this.elementName = elementName;
			this.indexCaptureName = indexCaptureName;
			this.capturedIndex = capturedIndex;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((capturedIndex == null) ? 0 : capturedIndex.hashCode());
			result = prime * result + ((elementName == null) ? 0 : elementName.hashCode());
			result = prime * result + ((indexCaptureName == null) ? 0 : indexCaptureName.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			IndexCapturePathElement other = (IndexCapturePathElement) obj;
			if (capturedIndex == null) {
				if (other.capturedIndex != null)
					return false;
			} else if (!capturedIndex.equals(other.capturedIndex))
				return false;
			if (elementName == null) {
				if (other.elementName != null)
					return false;
			} else if (!elementName.equals(other.elementName))
				return false;
			if (indexCaptureName == null) {
				if (other.indexCaptureName != null)
					return false;
			} else if (!indexCaptureName.equals(other.indexCaptureName))
				return false;
			return true;
		}

		public boolean matches(IndexCapturePathElement other) {
			if (this == other)
				return true;
			if (other == null)
				return false;
			if (elementName == null) {
				if (other.elementName != null)
					return false;
			} else if (!elementName.equals(other.elementName))
				return false;
			if (capturedIndex.isPresent()) {
				if (other.indexCaptureName.isPresent()) {//a number index always matches with an index label
					return true;
				}
				else if (other.capturedIndex.isPresent()) {//if the index number is specified on both then they have to match
					return this.capturedIndex.equals(other.capturedIndex);
				}
			}
			return true;
		}

		@Override
		public int compareTo(IndexCapturePathElement o) {
			return elementName.compareTo(o.elementName);
		}

		@Override
		public String toString() {
			return elementName + indexCaptureName.map(c->"["+c+"]").orElse("")
					+ capturedIndex.map(c->"["+c+"]").orElse("");
		}
	}

	public Optional<Integer> getLastIndex() {
		for (int i=elements.length-1;i>=0;i--) {
			if (elements[i].capturedIndex.isPresent()) {
				return elements[i].capturedIndex;
			}
		}
		return Optional.empty();
	}
}
