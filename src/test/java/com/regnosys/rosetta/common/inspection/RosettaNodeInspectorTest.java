package com.regnosys.rosetta.common.inspection;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.Processor;

import org.junit.jupiter.api.Test;
import org.mockito.internal.matchers.GreaterOrEqual;
import com.regnosys.rosetta.common.inspection.RosettaNodeInspector.Visitor;
import com.regnosys.rosetta.common.util.Pair;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.meta.RosettaMetaData;

class RosettaNodeInspectorTest {

    private final static Baz BAZ = new BazImpl("A", 4);
    private final static Bar BAR_1 = new BarImpl(1, Arrays.asList(BAZ));
    private final static Bar BAR_2 = new BarImpl(2, null);
    private final static Bar BAR_3 = new BarImpl(3, Collections.emptyList());

    @Test
    void shouldFindAllObjectPaths() {
        Foo foo = new FooImpl(Arrays.asList(BAR_1, BAR_2, BAR_3), BAZ, Faz.XYZ);

        List<PathObject<Object>> allPaths = new LinkedList<>();

        RosettaNodeInspector<PathObject<Object>> rosettaNodeInspector = new RosettaNodeInspector<>();
        Visitor<PathObject<Object>> addAllPathsVisitor = (node) -> allPaths.add(node.get());
        rosettaNodeInspector.inspect(PathObjectNode.root(foo), addAllPathsVisitor);

        assertThat(allPaths, hasSize(greaterThanOrEqualTo(15)));
        assertThat(allPaths.stream()
                        .map(o -> Pair.of(o.getHierarchicalPath().map(RosettaPath::buildPath).orElse(""), o.getObject()))
                        .collect(Collectors.toList()),
                hasItems(Pair.of("", foo),
                        Pair.of("bars(0)", BAR_1),
                        Pair.of("bars(0).a", BAR_1.getA()),
                        Pair.of("bars(0).bazs(0)", BAZ),
                        Pair.of("bars(0).bazs(0).b", BAZ.getB()),
                        Pair.of("bars(0).bazs(0).c", BAZ.getC()),
                        Pair.of("bars(1)", BAR_2),
                        Pair.of("bars(1).a", BAR_2.getA()),
                        Pair.of("bars(1).bazs(0)", null),
                        Pair.of("bars(2)", BAR_3),
                        Pair.of("bars(2).a", BAR_3.getA()),
                        Pair.of("baz", BAZ),
                        Pair.of("baz.b", BAZ.getB()),
                        Pair.of("baz.c", BAZ.getC()),
                        Pair.of("faz", Faz.XYZ)));
    }

    @Test
    void shouldFindAllTypePaths() {
        List<PathObject<Class<?>>> allPaths = new LinkedList<>();

        RosettaNodeInspector<PathObject<Class<?>>> rosettaNodeInspector = new RosettaNodeInspector<>();
        Visitor<PathObject<Class<?>>> addAllPathsVisitor = (node) -> allPaths.add(node.get());
        rosettaNodeInspector.inspect(PathTypeNode.root(Foo.class), addAllPathsVisitor);

        assertThat(allPaths, hasSize(greaterThanOrEqualTo(10)));
        List<String> collect = allPaths.stream()
                        .map(o -> o.getHierarchicalPath().map(RosettaPath::buildPath).orElse(""))
                        .collect(Collectors.toList());
		assertThat(collect,
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

    interface Foo extends RosettaModelObject{
		Faz getFaz();
		Baz getBaz();
		List<Bar> getBars();}
    
    private static class FooImpl implements Foo {
        private final List<Bar> bars;
        private final Baz baz;
        private final Faz faz;

        public FooImpl(List<Bar> bars, Baz baz, Faz faz) {
            this.bars = bars;
            this.baz = baz;
            this.faz = faz;
        }

        @Override
		public List<Bar> getBars() { return bars; }

        @Override
		public Baz getBaz() {
            return baz;
        }

        @Override
		public Faz getFaz() { return faz; }

        @Override
        public RosettaModelObjectBuilder toBuilder() {
            throw new UnsupportedOperationException();
        }

        @Override
		public void process(RosettaPath path, Processor processor) {
            throw new UnsupportedOperationException();
        }

		@Override
		public RosettaMetaData<? extends RosettaModelObject> metaData() {
			return null;
		}

		@Override
		public RosettaModelObject build() {
			return this;
		}

		@Override
		public Class<? extends RosettaModelObject> getType() {
			return Foo.class;
		}
    }

    interface Bar extends RosettaModelObject {

		List<Baz> getBazs();

		Integer getA();}

    private static class BarImpl implements Bar {
        private final Integer a;
        private final List<Baz> bazs;

        public BarImpl(Integer a, List<Baz> bazs) {
            this.a = a;
            this.bazs = bazs;
        }

        @Override
		public Integer getA() {
            return a;
        }

        @Override
		public List<Baz> getBazs() {
            return bazs;
        }

        @Override
        public RosettaModelObjectBuilder toBuilder() {
            throw new UnsupportedOperationException();
        }

        @Override
		public void process(RosettaPath path, Processor processor) {
            throw new UnsupportedOperationException();
        }

		@Override
		public RosettaMetaData<? extends RosettaModelObject> metaData() {
			return null;
		}

		@Override
		public RosettaModelObject build() {
			return this;
		}

		@Override
		public Class<? extends RosettaModelObject> getType() {
			return Bar.class;
		}
    }
    
    interface Baz extends RosettaModelObject {

		Integer getC();

		String getB();}

    private static class BazImpl implements Baz {
        private final String b;
        private final Integer c;

        public BazImpl(String b, Integer c) {
            this.b = b;
            this.c = c;
        }

        @Override
		public String getB() {
            return b;
        }

        @Override
		public Integer getC() {
            return c;
        }

        @Override
        public RosettaModelObjectBuilder toBuilder() {
            throw new UnsupportedOperationException();
        }

        @Override
		public void process(RosettaPath path, Processor processor) {
            throw new UnsupportedOperationException();
        }

		@Override
		public RosettaMetaData<? extends RosettaModelObject> metaData() {
			return null;
		}

		@Override
		public RosettaModelObject build() {
			return this;
		}

		@Override
		public Class<? extends RosettaModelObject> getType() {
			return Baz.class;
		}
    }

    private enum Faz {
        XYZ
    }

}