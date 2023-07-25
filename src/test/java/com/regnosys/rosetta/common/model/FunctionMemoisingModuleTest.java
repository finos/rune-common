package com.regnosys.rosetta.common.model;

import com.google.inject.*;
import com.google.inject.matcher.Matchers;
import com.google.inject.util.Modules;
import com.rosetta.model.lib.functions.RosettaFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class FunctionMemoisingModuleTest {

    public static final String COM_REGNOSYS_MODEL = "com.regnosys.rosetta.common.model";
    private Counter counter;

    @BeforeEach
    void setUp() {
        counter = new Counter();
    }

    @Test
    void checkEvaluateIsCalledOnlyOnce() {
        Injector injector = createInjector(COM_REGNOSYS_MODEL);
        Foo foo = injector.getInstance(Foo.class);
        foo.evaluate("xxx");
        foo.evaluate("xxx");
        assertThat(counter.count(), is(1));
    }

    @Test
    void checkEvaluateIsCalled750Times() {
        Injector injector = createInjector(COM_REGNOSYS_MODEL, Foo.class);
        Foo foo = injector.getInstance(Foo.class);
        IntStream.rangeClosed(0, 499).map(Math::abs).forEach(i -> foo.evaluate("xxx " + i));
        IntStream.rangeClosed(-499, 0).map(Math::abs).forEach(i -> foo.evaluate("xxx " + i));
        assertThat(counter.count(), is(750));
    }

    @Test
    void checkEvaluateIsNotCalledForImpl() {
        Injector injector = Guice.createInjector(Modules.combine(createModule(COM_REGNOSYS_MODEL), new AbstractModule() {
            @Override
            protected void configure() {
                bind(Foo.class).to(FooImpl.class);
            }
        }));
        Foo foo = injector.getInstance(Foo.class);
        foo.evaluate("xxx");
        foo.evaluate("xxx");
        assertThat(counter.count(), is(2));
    }

    @Test
    void checkNoCacheForFunctionOutsidePackage() {
        Injector injector = Guice.createInjector(createModule("cdm.base.math.functions"));
        Bar bar = injector.getInstance(Bar.class);
        bar.evaluate(BigDecimal.ONE);
        bar.evaluate(BigDecimal.TEN);
        bar.evaluate(BigDecimal.ZERO);
        bar.evaluate(BigDecimal.ONE);
        bar.evaluate(BigDecimal.TEN);
        bar.evaluate(BigDecimal.ZERO);
        assertThat(counter.count(), is(6));
    }

    @Test
    void checkCachedForFunctionInsidePackage() {
        Injector injector = Guice.createInjector(createModule(COM_REGNOSYS_MODEL));
        Bar abs = injector.getInstance(Bar.class);
        abs.evaluate(BigDecimal.ONE);
        abs.evaluate(BigDecimal.TEN);
        abs.evaluate(BigDecimal.ZERO);
        abs.evaluate(BigDecimal.ONE);
        abs.evaluate(BigDecimal.TEN);
        abs.evaluate(BigDecimal.ZERO);
        assertThat(counter.count(), is(3));
    }

    private Injector createInjector(String packageName, Class<? extends RosettaFunction>... debug) {
        Injector injector = Guice.createInjector(createModule(packageName, debug));
        return injector;
    }

    private AbstractModule createModule(String packageName, Class<? extends RosettaFunction>... debug) {
        return new AbstractModule() {
            @Override
            protected void configure() {
                install(new FunctionMemoisingModuleBuilder()
                        .setPackages(packageName)
                        .setDebugLoggingFunctions(debug)
                        .build());
                bind(Counter.class).toInstance(counter);
                binder().bindInterceptor(Matchers.only(Foo.class), FunctionMemoisingModule.isEvaluateMethod(),
                        x -> {
                            counter.incr();
                            return x.proceed();
                        });
            }
        };
    }

    static class Counter {
        int c = 0;

        int count() {
            return c;
        }

        void incr() {
            c++;
        }

    }

    @ImplementedBy(Foo.FooDefault.class)
    static abstract class Foo implements RosettaFunction {

        public Foo() {
        }

        public String evaluate(String arg) {
            String result = this.doEvaluate(arg);
            return result;
        }

        protected abstract String doEvaluate(String var1);

        public static class FooDefault extends Foo {
            @Inject
            Counter counter;

            public FooDefault() {
            }

            protected String doEvaluate(String arg) {
                counter.incr();
                return "Foo.FooDefault value";
            }
        }
    }

    @ImplementedBy(Bar.BarDefault.class)
    static abstract class Bar implements RosettaFunction {

        public Bar() {
        }

        public String evaluate(BigDecimal arg) {
            String result = this.doEvaluate(arg);
            return result;
        }

        protected abstract String doEvaluate(BigDecimal var1);

        public static class BarDefault extends Bar {
            @Inject
            Counter counter;

            public BarDefault() {
            }

            protected String doEvaluate(BigDecimal arg) {
                counter.incr();
                return "Bar.BarDefault value";
            }
        }
    }

    static class FooImpl extends Foo.FooDefault {
        public String evaluate(String arg) {
            return doEvaluate(arg);
        }
    }
}