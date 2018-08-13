package com.regnosys.rosetta.common.inspection;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static com.regnosys.rosetta.common.inspection.RosettaNodeInspector.*;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

class RosettaNodeInspectorTest {

    @Test
    void shouldFindAllPaths() {
        List<PathType> allPaths = new LinkedList<>();

        RosettaNodeInspector<PathType> rosettaNodeInspector = new RosettaNodeInspector<>();
        Visitor<PathType> addAllPathsVisitor = (node) -> allPaths.add(node.get());
        Visitor<PathType> noOpRootVisitor = (node) -> {};
        rosettaNodeInspector.inspect(new PathTypeNode(PathType.root(Foo.class)), addAllPathsVisitor, noOpRootVisitor);

        assertThat(allPaths, hasSize(8));
        assertThat(allPaths.stream().map(Object::toString).collect(Collectors.toList()),
                hasItems("Foo -> bars",
                        "Foo -> bars -> a",
                        "Foo -> bars -> bazs",
                        "Foo -> bars -> bazs -> b",
                        "Foo -> bars -> bazs -> c",
                        "Foo -> baz",
                        "Foo -> baz -> b",
                        "Foo -> baz -> c"));
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