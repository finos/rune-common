package com.regnosys.rosetta.common.inspection;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.Processor;

import org.junit.jupiter.api.Test;

import com.regnosys.rosetta.common.inspection.RosettaNodeInspector.Visitor;
import com.regnosys.rosetta.common.util.Pair;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.meta.RosettaMetaData;

class RosettaNodeInspectorTest {

    private final static Baz BAZ = new Baz("A", 4);
    private final static Bar BAR_1 = new Bar(1, Arrays.asList(BAZ));
    private final static Bar BAR_2 = new Bar(2, null);
    private final static Bar BAR_3 = new Bar(3, Collections.emptyList());

    @Test
    void shouldFindAllObjectPaths() {
        Foo foo = new Foo(Arrays.asList(BAR_1, BAR_2, BAR_3), BAZ, Faz.XYZ);

        List<PathObject<Object>> allPaths = new LinkedList<>();

        RosettaNodeInspector<PathObject<Object>> rosettaNodeInspector = new RosettaNodeInspector<>();
        Visitor<PathObject<Object>> addAllPathsVisitor = (node) -> allPaths.add(node.get());
        rosettaNodeInspector.inspect(PathObjectNode.root(foo), addAllPathsVisitor);

        assertThat(allPaths, hasSize(15));
        assertThat(allPaths.stream()
                        .map(o -> Pair.of(o.getHierarchicalPath().map(RosettaPath::buildPath).orElse(""), o.getObject()))
                        .collect(Collectors.toList()),
                hasItems(Pair.of("", foo),
                        Pair.of("bars(0)", BAR_1),
                        Pair.of("bars(0).a", BAR_1.a),
                        Pair.of("bars(0).bazs(0)", BAZ),
                        Pair.of("bars(0).bazs(0).b", BAZ.b),
                        Pair.of("bars(0).bazs(0).c", BAZ.c),
                        Pair.of("bars(1)", BAR_2),
                        Pair.of("bars(1).a", BAR_2.a),
                        Pair.of("bars(1).bazs(0)", null),
                        Pair.of("bars(2)", BAR_3),
                        Pair.of("bars(2).a", BAR_3.a),
                        Pair.of("baz", BAZ),
                        Pair.of("baz.b", BAZ.b),
                        Pair.of("baz.c", BAZ.c),
                        Pair.of("faz", Faz.XYZ)));
    }

    @Test
    void shouldFindAllTypePaths() {
        List<PathObject<Class<?>>> allPaths = new LinkedList<>();

        RosettaNodeInspector<PathObject<Class<?>>> rosettaNodeInspector = new RosettaNodeInspector<>();
        Visitor<PathObject<Class<?>>> addAllPathsVisitor = (node) -> allPaths.add(node.get());
        rosettaNodeInspector.inspect(PathTypeNode.root(Foo.class), addAllPathsVisitor);

        assertThat(allPaths, hasSize(10));
        assertThat(allPaths.stream()
                        .map(o -> o.getHierarchicalPath().map(RosettaPath::buildPath).orElse(""))
                        .collect(Collectors.toList()),
                hasItems("bars",
                        "bars.a",
                        "bars.bazs",
                        "bars.bazs.b",
                        "bars.bazs.c",
                        "baz",
                        "baz.b",
                        "baz.c",
                        "faz"));
    }

    @SuppressWarnings("unused")
    private static class Foo extends RosettaModelObject {
        private final List<Bar> bars;
        private final Baz baz;
        private final Faz faz;

        public Foo(List<Bar> bars, Baz baz, Faz faz) {
            this.bars = bars;
            this.baz = baz;
            this.faz = faz;
        }

        public List<Bar> getBars() { return bars; }

        public Baz getBaz() {
            return baz;
        }

        public Faz getFaz() { return faz; }

        @Override
        public RosettaModelObjectBuilder toBuilder() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void process(RosettaPath path, Processor processor) {
            throw new UnsupportedOperationException();
        }

		@Override
		public RosettaMetaData<? extends RosettaModelObject> metaData() {
			return null;
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
        protected void process(RosettaPath path, Processor processor) {
            throw new UnsupportedOperationException();
        }

		@Override
		public RosettaMetaData<? extends RosettaModelObject> metaData() {
			return null;
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
        protected void process(RosettaPath path, Processor processor) {
            throw new UnsupportedOperationException();
        }

		@Override
		public RosettaMetaData<? extends RosettaModelObject> metaData() {
			return null;
		}
    }

    private enum Faz {
        XYZ
    }

}