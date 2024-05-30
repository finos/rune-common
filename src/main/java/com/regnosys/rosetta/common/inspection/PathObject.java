package com.regnosys.rosetta.common.inspection;

/*-
 * #%L
 * Rune Common
 * %%
 * Copyright (C) 2018 - 2024 REGnosys
 * %%
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
 * #L%
 */

import com.google.common.base.Strings;
import com.rosetta.model.lib.path.RosettaPath;

import java.util.*;
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

    public PathObject(PathObject<T> parent, String accessor, int index, T t) {
        LinkedList<Element<T>> newElements = new LinkedList<>();
        newElements.addAll(parent.elements);
        newElements.add(new Element<>(accessor, index, t));
        this.elements = newElements;
    }

    private PathObject(LinkedList<Element<T>> elements) {
        this.elements = elements;
    }

    public Optional<RosettaPath> getHierarchicalPath() {
        String buildPath = buildPath();
        return Strings.isNullOrEmpty(buildPath) ?
                Optional.empty() :
                Optional.of(RosettaPath.valueOf(buildPath));
    }

    private String buildPath() {
        List<String> path = getPath();
        // remove root element, so it starts with the first accessor
        path.remove(0);
        return String.join(".", path);
    }

    public List<String> getPath() {
        return elements.stream()
                .map(e -> e.getAccessor() + (e.getIndex().map(i -> "(" + i + ")").orElse("")))
                .collect(Collectors.toList());
    }

    public LinkedList<T> getPathObjects() {
        return elements.stream().map(Element::getObject).collect(Collectors.toCollection(LinkedList::new));
    }

    public Optional<PathObject<T>> getParent() {
        if(elements.size() < 2)
            return Optional.empty();

        LinkedList<Element<T>> parent = new LinkedList<>(elements);
        parent.removeLast();
        return Optional.of(new PathObject<>(parent));
    }

    public T getObject() {
        return elements.getLast().getObject();
    }

    @Override
    public String toString() {
        return "PathObject{" +
                "elements=" + elements +
                '}';
    }

    private static class Element<T> {
        private final String accessor;
        private final Optional<Integer> index;
        private final T object;

        Element(String accessor, T object) {
            this.accessor = accessor;
            this.index = Optional.empty();
            this.object = object;
        }

        Element(String accessor, int index, T object) {
            this.accessor = accessor;
            this.index = Optional.of(index);
            this.object = object;
        }

        String getAccessor() {
            return accessor;
        }

        Optional<Integer> getIndex() {
            return index;
        }

        T getObject() {
            return object;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Element<?> element = (Element<?>) o;
            return Objects.equals(accessor, element.accessor) &&
                    Objects.equals(index, element.index) &&
                    Objects.equals(object, element.object);
        }

        @Override
        public int hashCode() {
            return Objects.hash(accessor, index, object);
        }

        @Override
        public String toString() {
            return "Element{" +
                    "accessor='" + accessor + '\'' +
                    ", index=" + index +
                    ", object=" + object +
                    '}';
        }
    }
}
