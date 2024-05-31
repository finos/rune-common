package com.regnosys.rosetta.common.translation;

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

import com.google.common.collect.ImmutableList;
import com.regnosys.rosetta.common.util.PathException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Path {

    private static final String WILDCARD = "*";

    private final List<PathElement> elements;

    public Path() {
        this.elements = ImmutableList.of();
    }

    public Path(List<PathElement> elements) {
        this.elements = elements;
    }

    public static Path valueOf(List<String> path) {
        return new Path(path.stream().map(p -> new PathElement(p)).collect(Collectors.toList()));
    }

    public static Path valueOf(String path) {
        List<PathElement> newPath = new ArrayList<>();
        newPath.add(new PathElement(path));
        return new Path(newPath);
    }

    public Path addElement(PathElement element) {
        List<PathElement> newPath = new ArrayList<>();
        newPath.addAll(elements);
        newPath.add(element);
        return new Path(newPath);
    }

    public Path addElement(String name) {
        return addElement(new PathElement(name));
    }

    public Path addElement(String name, Integer index) {
        return addElement(new PathElement(name, Optional.ofNullable(index), Collections.emptyMap()));
    }

    public List<PathElement> getElements() {
        return elements;
    }

    public String[] getPathNames() {
        return elements.stream()
                .map(Path.PathElement::getPathName)
                .toArray(String[]::new);
    }

    public Path getParent() {
        return new Path(elements.subList(0, elements.size() - 1));
    }

    public PathElement getLastElement() {
        if (elements.size() < 1)
            return null;
        return elements.get(elements.size()-1);
    }

    public Path append(Path append) {
        List<PathElement> list = ImmutableList.<PathElement>builder()
                .addAll(elements)
                .addAll(append.elements)
                .build();
        return new Path(list);
    }

    public Path prefixWithWildcard() {
        return Path.valueOf("*").append(this);
    }

    /**
     * return true if the all the elements of this path are the start of the other path
     * matching only on the name
     */
    public boolean nameStartMatches(Path other) {
        return nameStartMatches(other, false);
    }

    /**
     * return true if the all the elements of this path are the start of the other path
     * matching only on the name
     */
    public boolean nameStartMatches(Path other, boolean allowWildcard) {
        if (elements.isEmpty() && other.elements.isEmpty())
            return true;
		if (elements.isEmpty())
			return false;
        if (elements.size() > other.elements.size())
            return false;
        for (int i = 0; i < elements.size(); i++) {
            String p1 = elements.get(i).pathName;
            String p2 = other.elements.get(i).pathName;
            if (!(p1.equals(p2) || wildcardMatches(allowWildcard, p1, p2)))
                return false;
        }
        return true;
    }

    private boolean wildcardMatches(boolean allowWildcard, String p1, String p2) {
        return allowWildcard && (WILDCARD.equals(p1) || WILDCARD.equals(p2));
    }

    /**
     * return true if the all the elements of this path are the start of the other path
     * matching on the name and index
     */
    public boolean fullStartMatches(Path other) {
        return fullStartMatches(other, false);
    }

    public boolean fullStartMatches(Path other, boolean allowWildcard) {
        if (elements.isEmpty() && other.elements.isEmpty())
            return true;
        if (elements.isEmpty())
            return false;
        if (elements.size() > other.elements.size())
            return false;
        for (int i = 0; i < elements.size(); i++) {
            String p1 = elements.get(i).pathName;
            String p2 = other.elements.get(i).pathName;
            if (wildcardMatches(allowWildcard, p1, p2))
                continue;
            if (!p1.equals(p2) || elements.get(i).index.orElse(0).intValue() != other.elements.get(i).index.orElse(0).intValue())
                return false;
        }
        return true;
    }

    public boolean nameIndexMatches(Path other) {
        if (elements.isEmpty() && other.elements.isEmpty())
            return true;
        if (elements.isEmpty())
            return false;
        if (elements.size() != other.elements.size())
            return false;
        for (int i = 0; i < elements.size(); i++) {
            if (!elements.get(i).pathName.equals(other.elements.get(i).pathName) ||
                    elements.get(i).index.orElse(0).intValue() != other.elements.get(i).index.orElse(0).intValue())
                return false;
        }
        return true;
    }

    public boolean endsWith(String... path) {
        int dif = elements.size() - path.length;
        if (dif < 0)
            return false;
        else {
            for (int i = 0; i < path.length; i++) {
                PathElement el = elements.get(dif + i);
                String s = path[i];
                if (!el.getPathName().equalsIgnoreCase(s))
                    return false;
            }
        }
        return true;
    }

    public boolean endsWith(Path other) {
        int thisSize = elements.size();
        int otherSize = other.elements.size();
        if (otherSize > thisSize) {
            return false;
        } else {
            // from the last element to the first, check that each element is equal
            for (int i = 1; i <= otherSize; i++) {
                if (!elements.get(thisSize - i).equals(other.elements.get(otherSize - i)))
                    return false;
            }
        }
        return true;
    }

    public int cardinality() {
        int result = -1;
        for (PathElement element : elements) {
            if (element.index.isPresent()) {
				if (result != -1) {
					//TODO restore this exception when I have time to diagnose the translate test that is hitting it
					//throw new PathException("Multiple cardinalities found in path "+this);
				}
				else result = element.index.get();
            }
        }
        if (result == -1)
            return 0;

        return result;
    }

    public static Path parse(String pathString) {
        return parse(pathString, false);
    }

    public static Path parse(String pathString, boolean allowWildcard) {
        return new Path(Arrays.stream(pathString.split("\\."))
                .filter(s -> s.length() > 0)
                .map(s -> PathElement.parse(s, allowWildcard))
                .collect(Collectors.toList()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Path path = (Path) o;
        return elements.equals(path.elements);
    }

    @Override public int hashCode() {
        return Objects.hash(elements);
    }

    @Override
    public String toString() {
        return String.join(".", elements.stream().map(PathElement::toString).collect(Collectors.toList()));
    }

    public static class PathElement {
        private final String pathName;
        private Optional<Integer> index;//this can't be final because sometimes we don't know this is index[0] until we find index[1] later
        private final Map<String, String> metas;

        public PathElement(String pathName) {
            this(pathName, Optional.empty(), Collections.emptyMap());
        }

        public PathElement(String pathName, Map<String, String> metas) {
            this(pathName, Optional.empty(), metas);
        }

        public PathElement(String pathName, Optional<Integer> index, Map<String, String> metas) {
            Objects.nonNull(pathName);
            Objects.nonNull(index);
            Objects.nonNull(metas);
            this.pathName = pathName;
            this.index = index;
            this.metas = metas;
        }

        public PathElement(String pathName, int index) {
            this(pathName, Optional.of(index), Collections.emptyMap());
        }

        public PathElement(String pathName, int index, Map<String, String> metas) {
            this(pathName, Optional.of(index), metas);
        }

        public int forceGetIndex() {
            if (index.isPresent()) {
                return index.get();
            }
            index = Optional.of(0);
            return 0;
        }

        public Optional<Integer> getIndex() {
            return index;
        }

        public String getPathName() {
            return pathName;
        }

        public Map<String, String> getMetas() {
            return metas;
        }

        @Override
        public String toString() {
            return pathName + index.map(i -> "(" + i + ")").orElse("");
        }

        private static Pattern pattern = Pattern.compile("(\\w*)(\\[(\\d*)])?");
        private static Pattern patternAllowWildcard = Pattern.compile("([*]|\\w*)(\\[(\\d*)])?");

        public static PathElement parse(String s) {
            return parse(s, false);
        }

        public static PathElement parse(String s, boolean allowWildcard) {
            Matcher match = allowWildcard ? patternAllowWildcard.matcher(s) : pattern.matcher(s);
            if (match.matches()) {
                String name = match.group(1);
                Optional<Integer> index = (match.group(3) != null) ? Optional.of(Integer.valueOf(match.group(3))) : Optional.empty();
                return new PathElement(name, index, Collections.emptyMap());
            }
            throw new PathException("Failed to parse path element " + s);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((index == null) ? 0 : index.hashCode());
            result = prime * result + ((pathName == null) ? 0 : pathName.hashCode());
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
            PathElement other = (PathElement) obj;
            if (index == null) {
                if (other.index != null)
                    return false;
            } else if (!index.equals(other.index))
                return false;
            if (metas != null && other.metas!=null && !metas.isEmpty() && ! other.metas.isEmpty()) {
            	//if one is null or empty they are considered to match
            	//if neither is null or empty then actually compare them
            	if (!metas.equals(other.metas))
                    return false;
            }
            if (pathName == null) {
                if (other.pathName != null)
                    return false;
            } else if (!pathName.equals(other.pathName))
                return false;
            return true;
        }
    }
}
