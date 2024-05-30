package com.regnosys.rosetta.common.model;

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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.matcher.Matcher;
import com.google.inject.matcher.Matchers;
import com.rosetta.model.lib.functions.RosettaFunction;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import static com.google.inject.matcher.Matchers.subclassesOf;

public class FunctionMemoisingModule extends AbstractModule {

    public static final String DEFAULT_SUFFIX = "Default";
    public static final String EVALUATE_METHOD_NAME = "evaluate";
    private final Set<String> packages;
    private final Set<String> debugFunctions;

    public FunctionMemoisingModule(Set<String> packages, Set<String> debugFunctions) {
        this.packages = packages;
        this.debugFunctions = debugFunctions;
    }

    @Override
    protected void configure() {
        CacheBuilder<Object, Object> cacheBuilder = configureCacheBuilder();
        Matcher<Class> classMatcher = createClassMatcher();
        binder().bindInterceptor(
                classMatcher,
                isEvaluateMethod(),
                new CachingMethodInterceptor(cacheBuilder, debugFunctions)
        );
    }

    private static CacheBuilder<Object, Object> configureCacheBuilder() {
        CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder();
        cacheBuilder.expireAfterAccess(Duration.of(5, ChronoUnit.MINUTES));
        cacheBuilder.maximumSize(250);
        return cacheBuilder;
    }

    @SuppressWarnings("rawtypes")
    private Matcher<Class> createClassMatcher() {
        Matcher<Class> classMatcher = subclassesOf(RosettaFunction.class)
                .and(isDefaultImpl());

        for (String aPackage : packages) {
            classMatcher = classMatcher.and(Matchers.inSubpackage(aPackage));
        }
        return classMatcher;
    }

    @VisibleForTesting
    public static AbstractMatcher<Method> isEvaluateMethod() {
        return new AbstractMatcher<Method>() {
            @Override
            public boolean matches(Method method) {
                return method.getName().equals(EVALUATE_METHOD_NAME) && !method.isSynthetic();
            }
        };
    }

    private static Matcher<Class> isDefaultImpl() {
        return new AbstractMatcher<Class>() {
            @Override
            public boolean matches(Class aClass) {
                return aClass.getName().endsWith(DEFAULT_SUFFIX);
            }
        };
    }
}
