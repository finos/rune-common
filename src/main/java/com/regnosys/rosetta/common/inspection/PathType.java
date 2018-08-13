package com.regnosys.rosetta.common.inspection;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.regnosys.rosetta.common.inspection.RosettaReflectionsUtil.getReturnType;

public class PathType {

    public static PathType root(Class<?> type) {
        return new PathType(type.getSimpleName(), type);
    }

    private final LinkedList<Element> elements;

    private PathType(String accessor, Class<?> type) {
        LinkedList<Element> newElements = new LinkedList<>();
        newElements.add(new Element(accessor, type));
        this.elements = newElements;

    }

    public PathType(PathType parent, Method method) {
        LinkedList<Element> newElements = new LinkedList<>();
        newElements.addAll(parent.elements);
        newElements.add(new Element(attrName(method), getReturnType(method)));
        this.elements = newElements;

    }

    public List<String> getPath() {
        return elements.stream().map(Element::getAccessor).collect(Collectors.toList());
    }

    public List<Class<?>> getPathTypes() {
        return elements.stream().map(Element::getType).collect(Collectors.toList());
    }

    public Class<?> getType() {
        return elements.getLast().getType();
    }

    private String attrName(Method method) {
        String attrName = method.getName().replace("get", "");
        return Character.toLowerCase(attrName.charAt(0)) + attrName.substring(1);
    }

    @Override
    public String toString() {
        return String.join(" -> ", getPath());
    }

    private static class Element {
        private final String accessor;
        private final Class<?> type;

        public Element(String accessor, Class<?> type) {
            this.accessor = accessor;
            this.type = type;
        }

        public String getAccessor() {
            return accessor;
        }

        public Class<?> getType() {
            return type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Element element = (Element) o;
            return Objects.equals(accessor, element.accessor) &&
                    Objects.equals(type, element.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(accessor, type);
        }

        @Override
        public String toString() {
            return "Element{" +
                    "accessor='" + accessor + '\'' +
                    ", type=" + type +
                    '}';
        }
    }
}
