package com.regnosys.rosetta.common.inspection;

import com.regnosys.rosetta.common.util.HierarchicalPath;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static com.regnosys.rosetta.common.inspection.RosettaNodeInspector.*;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

class RosettaNodeInspectorTest {

    private final static Baz BAZ = new Baz("A", 4);
    private final static Bar BAR_1 = new Bar(1, Arrays.asList(BAZ));
    private final static Bar BAR_2 = new Bar(2, null);
    private final static Bar BAR_3 = new Bar(3, Collections.emptyList());

    @Test
    void shouldBlah() {
        Foo foo = new Foo(Arrays.asList(BAR_1, BAR_2, BAR_3), BAZ);

        List<PathObject<Object>> allPaths = new LinkedList<>();

        RosettaNodeInspector<PathObject<Object>> rosettaNodeInspector = new RosettaNodeInspector<>();
        Visitor<PathObject<Object>> addAllPathsVisitor = (node) -> allPaths.add(node.get());
        rosettaNodeInspector.inspect(PathObjectNode.root(foo), addAllPathsVisitor);

        assertThat(allPaths, hasSize(14));
        assertThat(allPaths.stream()
                        .map(o -> new ImmutablePair<>(o.getHierarchicalPath().map(HierarchicalPath::buildPath).orElse(""), o.getObject()))
                        .collect(Collectors.toList()),
                hasItems(new ImmutablePair<>("", foo),
                        new ImmutablePair<>("bars(0)", BAR_1),
                        new ImmutablePair<>("bars(0).a", BAR_1.a),
                        new ImmutablePair<>("bars(0).bazs(0)", BAZ),
                        new ImmutablePair<>("bars(0).bazs(0).b", BAZ.b),
                        new ImmutablePair<>("bars(0).bazs(0).c", BAZ.c),
                        new ImmutablePair<>("bars(1)", BAR_2),
                        new ImmutablePair<>("bars(1).a", BAR_2.a),
                        new ImmutablePair<>("bars(1).bazs(0)", null),
                        new ImmutablePair<>("bars(2)", BAR_3),
                        new ImmutablePair<>("bars(2).a", BAR_3.a),
                        new ImmutablePair<>("baz", BAZ),
                        new ImmutablePair<>("baz.b", BAZ.b),
                        new ImmutablePair<>("baz.c", BAZ.c)));
    }

    @Test
    void shouldFindAllPaths() {
        List<PathObject<Class<?>>> allPaths = new LinkedList<>();

        RosettaNodeInspector<PathObject<Class<?>>> rosettaNodeInspector = new RosettaNodeInspector<>();
        Visitor<PathObject<Class<?>>> addAllPathsVisitor = (node) -> allPaths.add(node.get());
        Visitor<PathObject<Class<?>>> noOpRootVisitor = (node) -> {};
        rosettaNodeInspector.inspect(PathTypeNode.root(Foo.class), addAllPathsVisitor, noOpRootVisitor);

        assertThat(allPaths, hasSize(8));
        assertThat(allPaths.stream()
                        .map(o -> o.getHierarchicalPath().map(HierarchicalPath::buildPath).orElse(""))
                        .collect(Collectors.toList()),
                hasItems("bars",
                        "bars.a",
                        "bars.bazs",
                        "bars.bazs.b",
                        "bars.bazs.c",
                        "baz",
                        "baz.b",
                        "baz.c"));
    }

    @SuppressWarnings("unused")
    private static class Foo extends RosettaModelObject {
        private final List<Bar> bars;
        private final Baz baz;

        public Foo(List<Bar> bars, Baz baz) {
            this.bars = bars;
            this.baz = baz;
        }

        public List<Bar> getBars() {
            return bars;
        }

        public Baz getBaz() {
            return baz;
        }

        @Override
        public RosettaModelObjectBuilder toBuilder() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected int rosettaKeyValueHashCode() {
            throw new UnsupportedOperationException();
        }
    }

    @SuppressWarnings("unused")
    private static class Bar extends RosettaModelObject {
        private final Integer a;
        private final List<Baz> bazs;

        public Bar(Integer a, List<Baz> bazs) {
            this.a = a;
            this.bazs = bazs;
        }

        public Integer getA() {
            return a;
        }

        public List<Baz> getBazs() {
            return bazs;
        }

        @Override
        public RosettaModelObjectBuilder toBuilder() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected int rosettaKeyValueHashCode() {
            throw new UnsupportedOperationException();
        }
    }

    @SuppressWarnings("unused")
    private static class Baz extends RosettaModelObject {
        private final String b;
        private final Integer c;

        public Baz(String b, Integer c) {
            this.b = b;
            this.c = c;
        }

        public String getB() {
            return b;
        }

        public Integer getC() {
            return c;
        }

        @Override
        public RosettaModelObjectBuilder toBuilder() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected int rosettaKeyValueHashCode() {
            throw new UnsupportedOperationException();
        }
    }
}