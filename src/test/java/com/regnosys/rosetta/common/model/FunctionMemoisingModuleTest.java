package com.regnosys.rosetta.common.model;

/*-
 * ==============
 * Rosetta Common
 * --------------
 * Copyright (C) 2018 - 2024 REGnosys
 * --------------
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

/*-
 * #%L
 * Rosetta Common
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

    @SafeVarargs
    private final Injector createInjector(String packageName, Class<? extends RosettaFunction>... debug) {
        return Guice.createInjector(createModule(packageName, debug));
    }

    @SafeVarargs
    private final AbstractModule createModule(String packageName, Class<? extends RosettaFunction>... debug) {
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

        public void evaluate(String arg) {
            this.doEvaluate(arg);
        }

        protected abstract void doEvaluate(String var1);

        public static class FooDefault extends Foo {
            @Inject
            Counter counter;

            protected void doEvaluate(String arg) {
                counter.incr();
            }
        }
    }

    @ImplementedBy(Bar.BarDefault.class)
    static abstract class Bar implements RosettaFunction {

        public void evaluate(BigDecimal arg) {
            this.doEvaluate(arg);
        }

        protected abstract void doEvaluate(BigDecimal var1);

        public static class BarDefault extends Bar {
            @Inject
            Counter counter;

            protected void doEvaluate(BigDecimal arg) {
                counter.incr();
            }
        }
    }

    static class FooImpl extends Foo.FooDefault {
        public void evaluate(String arg) {
            doEvaluate(arg);
        }
    }
}
