package io.github.dimkich.integration.testing.expression;

import eu.ciechanowiec.sneakyfun.SneakyBiPredicate;
import eu.ciechanowiec.sneakyfun.SneakyConsumer;
import eu.ciechanowiec.sneakyfun.SneakyFunction;
import eu.ciechanowiec.sneakyfun.SneakyPredicate;
import io.github.dimkich.integration.testing.expression.wrapper.*;
import lombok.SneakyThrows;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;

/**
 * A central factory for creating dynamic functional interfaces (Predicates, Consumers, Functions)
 * from string-based Java expressions.
 *
 * <p>This factory utilizes <b>lazy compilation</b> and global caching. Expressions are compiled
 * into JVM bytecode only upon their first invocation and stored in a {@link ConcurrentHashMap}
 * to ensure high performance for recurring instrumentation rules.</p>
 *
 * <p>The engine supports complex Java syntax including:
 * <ul>
 *   <li>Control flow: {@code if (o.asList().isEmpty()) o.asList().add('init')}</li>
 *   <li>Loops: {@code for(int i=0; i<3; i++) o.asList().add(String.valueOf(i))}</li>
 *   <li>Arithmetic & Casting: {@code o.asInt() * 2} or {@code (String)o.get()}</li>
 *   <li>Static API calls: {@code java.lang.Math.max(o.asInt(), 100)}</li>
 * </ul></p>
 */
public class ExpressionFactory {
    /**
     * Global cache for compiled expression delegates.
     */
    private static final Map<String, Object> CACHE = new ConcurrentHashMap<>();

    /**
     * Creates a {@link PointcutMatch} for ByteBuddy instrumentation.
     *
     * @param exp the pointcut expression.
     *            Available variables: {@code t} ({@link TypeDescriptionWrapper}),
     *            {@code m} ({@link MethodDescriptionWrapper}).
     * @return a match object containing type and method junctions.
     * @example {@code "t.inherits('java.util.List') && m.isPublic() && m.name('add')"}
     */
    public static PointcutMatch createPointcutMatch(String exp) {
        BiPredicate<TypeDescriptionWrapper, MethodDescriptionWrapper> lazy = new BiPredicate<>() {
            private volatile SneakyBiPredicate<TypeDescriptionWrapper, MethodDescriptionWrapper, Exception> delegate;

            @Override
            @SneakyThrows
            public boolean test(TypeDescriptionWrapper t, MethodDescriptionWrapper m) {
                if (delegate == null) {
                    delegate = getOrCompile("pointcut.predicate:" + exp, () -> ExpressionBuilder.<SneakyBiPredicate<TypeDescriptionWrapper, MethodDescriptionWrapper, Exception>>builder()
                            .expression(exp)
                            .interfaceType(SneakyBiPredicate.class)
                            .returnType(boolean.class)
                            .param("t", TypeDescriptionWrapper.class)
                            .param("m", MethodDescriptionWrapper.class)
                            .build());
                }
                return delegate.test(t, m);
            }
        };
        return new PointcutMatch(PointcutRegistry.register(), new TypeJunction(lazy), new MethodJunction(lazy));
    }

    /**
     * Creates a {@link BiPredicate} for intercepting method calls with arguments.
     *
     * @param exp the expression logic.
     *            Available variables: {@code o} ({@link ObjectWrapper} - target),
     *            {@code a} ({@link ArgsWrapper} - arguments).
     * @return a predicate that evaluates the expression at runtime.
     * @example {@code "!o.isNull() && a.size() > 0 && a.arg(0).get() instanceof String"}
     */
    public static BiPredicate<Object, Object[]> createInvokePredicate(String exp) {
        return new BiPredicate<>() {
            private volatile SneakyBiPredicate<ObjectWrapper, ArgsWrapper, Exception> delegate;

            @Override
            @SneakyThrows
            public boolean test(Object o, Object[] a) {
                if (delegate == null) {
                    delegate = getOrCompile("invoke.predicate:" + exp, () -> ExpressionBuilder.<SneakyBiPredicate<ObjectWrapper, ArgsWrapper, Exception>>builder()
                            .expression(exp)
                            .interfaceType(SneakyBiPredicate.class)
                            .returnType(boolean.class)
                            .param("o", ObjectWrapper.class)
                            .param("a", ArgsWrapper.class)
                            .build());
                }
                return delegate.test(new ObjectWrapper(o), new ArgsWrapper(a));
            }
        };
    }

    /**
     * Creates a {@link Predicate} to evaluate a single object.
     *
     * @param exp the expression logic. Available variable: {@code o} ({@link ObjectWrapper}).
     * @return a predicate wrapping the object and evaluating the expression.
     * @example {@code "o.isInstance(java.util.ArrayList.class) && o.asList().size() > 0"}
     */
    public static Predicate<Object> createObjectPredicate(String exp) {
        return new Predicate<>() {
            private volatile SneakyPredicate<ObjectWrapper, Exception> delegate;

            @Override
            @SneakyThrows
            public boolean test(Object o) {
                if (delegate == null) {
                    delegate = getOrCompile("object.predicate:" + exp, () -> ExpressionBuilder.<SneakyPredicate<ObjectWrapper, Exception>>builder()
                            .expression(exp)
                            .interfaceType(SneakyPredicate.class)
                            .returnType(boolean.class)
                            .param("o", ObjectWrapper.class)
                            .build());
                }
                return delegate.test(new ObjectWrapper(o));
            }
        };
    }

    /**
     * Creates a {@link Consumer} to execute side-effect operations on an object.
     *
     * @param exp the expression logic (supports multi-statement blocks).
     *            Available variable: {@code o} ({@link ObjectWrapper}).
     * @example {@code "{ o.asList().add('item1'); o.call('setModified', true); }"}
     */
    public static Consumer<Object> createObjectConsumer(String exp) {
        return new Consumer<>() {
            private volatile SneakyConsumer<ObjectWrapper, Exception> delegate;

            @Override
            @SneakyThrows
            public void accept(Object o) {
                if (delegate == null) {
                    delegate = getOrCompile("object.consumer:" + exp, () -> ExpressionBuilder.<SneakyConsumer<ObjectWrapper, Exception>>builder()
                            .expression(exp)
                            .interfaceType(SneakyConsumer.class)
                            .returnType(void.class)
                            .param("o", ObjectWrapper.class)
                            .build());
                }
                delegate.accept(new ObjectWrapper(o));
            }
        };
    }

    /**
     * Creates a {@link Function} that transforms an object into a result of type {@code T}.
     *
     * @param exp the expression logic. Available variable: {@code o} ({@link ObjectWrapper}).
     * @param res the expected return type class.
     * @param <T> the type of the result.
     * @return a function that converts the object via the expression.
     * @example {@code "o.asString().toUpperCase()"} returning {@code String.class}
     */
    public static <T> Function<Object, T> createObjectFunction(String exp, Class<T> res) {
        return new Function<>() {
            private volatile SneakyFunction<ObjectWrapper, T, Exception> delegate;

            @Override
            @SneakyThrows
            public T apply(Object o) {
                if (delegate == null) {
                    String key = String.format("object.function[%s]:%s", res.getName(), exp);
                    delegate = getOrCompile(key, () -> ExpressionBuilder.<SneakyFunction<ObjectWrapper, T, Exception>>builder()
                            .expression(exp)
                            .interfaceType(SneakyFunction.class)
                            .returnType(res)
                            .param("o", ObjectWrapper.class)
                            .build());
                }
                return delegate.apply(new ObjectWrapper(o));
            }
        };
    }

    /**
     * Internal helper to manage the expression cache and invoke the compiler.
     */
    @SuppressWarnings("unchecked")
    private static <V> V getOrCompile(String key, Supplier<V> compiler) {
        return (V) CACHE.computeIfAbsent(key, k -> compiler.get());
    }
}
