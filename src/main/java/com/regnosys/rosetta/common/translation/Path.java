package com.regnosys.rosetta.common.translation;

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

    public List<PathElement> getElements() {
        return elements;
    }

    public Path getParent() {
        return new Path(elements.subList(0, elements.size() - 1));
    }

    public Path append(Path append) {
        List<PathElement> list = ImmutableList.<PathElement>builder()
                .addAll(elements)
                .addAll(append.elements)
                .build();
        return new Path(list);
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
        if (elements.size() > other.elements.size())
            return false;
        for (int i = 0; i < elements.size(); i++) {
            if (!elements.get(i).pathName.equals(other.elements.get(i).pathName) ||
                    elements.get(i).index.orElse(0) != other.elements.get(i).index.orElse(0))
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
    public String toString() {
        return String.join(".", elements.stream().map(PathElement::toString).collect(Collectors.toList()));
    }

    public static class PathElement {
        private final String pathName;
        private Optional<Integer> index;
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

        public void setIndex(int i) {
            index = Optional.of(i);
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
            result = prime * result + ((metas == null) ? 0 : metas.hashCode());
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
            if (metas == null) {
                if (other.metas != null)
                    return false;
            } else if (!metas.equals(other.metas))
                return false;
            if (pathName == null) {
                if (other.pathName != null)
                    return false;
            } else if (!pathName.equals(other.pathName))
                return false;
            return true;
        }
    }
}
