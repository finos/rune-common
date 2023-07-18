package com.regnosys.rosetta.common.model;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.matcher.Matcher;
import com.google.inject.matcher.Matchers;
import com.rosetta.model.lib.functions.IQualifyFunctionExtension;
import com.rosetta.model.lib.functions.RosettaFunction;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import static com.google.inject.matcher.Matchers.not;
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
                .and(isDefaultImpl())
                .and(not(subclassesOf(IQualifyFunctionExtension.class)));

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
                return method.getName().equals(EVALUATE_METHOD_NAME);
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
