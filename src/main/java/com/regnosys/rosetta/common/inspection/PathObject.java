package com.regnosys.rosetta.common.inspection;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PathObject<T> {

    private final LinkedList<Element<T>> elements;

    PathObject(String accessor, T t) {
        LinkedList<Element<T>> newElements = new LinkedList<>();
        newElements.add(new Element<>(accessor, t));
        this.elements = newElements;
    }

    public PathObject(PathObject<T> parent, String accessor, T t) {
        LinkedList<Element<T>> newElements = new LinkedList<>();
        newElements.addAll(parent.elements);
        newElements.add(new Element<>(accessor, t));
        this.elements = newElements;
    }

    public String buildPath() {
        List<String> path = getPath();
        // remove root element, so it starts with the first accessor
        path.remove(0);
        return String.join(".", path);
    }

    public List<String> getPath() {
        return elements.stream().map(Element::getAccessor).collect(Collectors.toList());
    }

    public List<T> getPathObjects() {
        return elements.stream().map(Element::getObject).collect(Collectors.toList());
    }

    public T getObject() {
        return elements.getLast().getObject();
    }

    @Override
    public String toString() {
        return String.join(" -> ", getPath());
    }

    private static class Element<T> {
        private final String accessor;
        private final T object;

        Element(String accessor, T object) {
            this.accessor = accessor;
            this.object = object;
        }

        String getAccessor() {
            return accessor;
        }

        T getObject() {
            return object;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Element element = (Element) o;
            return Objects.equals(accessor, element.accessor) &&
                    Objects.equals(object, element.object);
        }

        @Override
        public int hashCode() {
            return Objects.hash(accessor, object);
        }

        @Override
        public String toString() {
            return "Element{" +
                    "accessor='" + accessor + '\'' +
                    ", object=" + object +
                    '}';
        }
    }
}
