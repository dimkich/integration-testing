package io.github.dimkich.integration.testing.wait.completion;

import java.lang.annotation.*;
import java.util.function.Consumer;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Defines a strategy to track and await completion of asynchronous "future-like" objects.
 *
 * <p>A "future-like" object is any instance that represents a pending operation (e.g.,
 * {@code CompletableFuture}, Apache MINA {@code IoFuture}, or a custom Task class).
 * This annotation instructs the framework to intercept the creation of such objects
 * and block the test execution until they are complete.</p>
 *
 * <h3>Usage Examples:</h3>
 * <ul>
 *   <li><b>Real-world (Apache MINA):</b> Intercept all IoFuture constructors and call a specific method to wait.
 *       <pre>{@code
 *       @FutureLikeAwait(
 *           pointcut = "t.inherits('org.apache.mina.core.future.IoFuture') && m.isConstructor()",
 *           await = "o.call('awaitUninterruptibly')"
 *       )}</pre>
 *   </li>
 *   <li><b>Conditional Tracking:</b> Track only specific instances based on their class.
 *       <pre>{@code
 *       @FutureLikeAwait(
 *           pointcut = "t.inherits('com.app.BaseTask') && m.isConstructor()",
 *           when = "o.isSameClass(com.app.MyTask.class)",
 *           await = "o.call('await')"
 *       )}</pre>
 *   </li>
 *   <li><b>Annotation-based:</b> Target factory methods marked with a specific annotation.
 *       <pre>{@code
 *       @FutureLikeAwait(
 *           pointcut = "t.inherits('com.app.BaseTask') && m.name('create') && m.ann('com.app.Tracked')",
 *           awaitConsumer = MyCustomAwaitConsumer.class
 *       )}</pre>
 *   </li>
 *   <li><b>Package-wide matching:</b>
 *       <pre>{@code
 *       @FutureLikeAwait(
 *           pointcut = "t.packageStartsWith('com.app.async') && m.ann('com.app.AsyncMarker')",
 *           await = "o.call('await')"
 *       )}</pre>
 *   </li>
 * </ul>
 */
@Inherited
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(FutureLikeAwait.List.class)
public @interface FutureLikeAwait {
    /**
     * A DSL expression to identify the "injection points" (constructors or methods)
     * that produce the objects to be tracked.
     *
     * <p>Available variables: {@code t} (TypeDescriptionWrapper), {@code m} (MethodDescriptionWrapper).</p>
     * @see io.github.dimkich.integration.testing.expression.ExpressionFactory
     */
    String pointcut();

    /**
     * A DSL expression evaluated at runtime to determine if a specific instance should be tracked.
     *
     * <p>Available variables: {@code o} (ObjectWrapper - the created object), {@code a} (ArgsWrapper - factory/constructor arguments).</p>
     * <p>Default is {@code "true"} (track all matched instances).</p>
     */
    String when() default "true";

    /**
     * A DSL expression representing the blocking action to perform on each tracked instance.
     *
     * <p>Typically used to call a method like {@code join()}, {@code get()}, or {@code await()}.
     * This is a convenient alternative to providing a full {@link #awaitConsumer()} class.</p>
     *
     * <p>Available variable: {@code o} (ObjectWrapper - the tracked instance).</p>
     * @example {@code await = "o.call('awaitUninterruptibly')"}
     */
    String await() default "";

    /**
     * A custom implementation of {@link Consumer} that defines the waiting logic.
     *
     * <p>The consumer receives the tracked instance and must block the thread until
     * the asynchronous operation is finished. Use this for complex scenarios
     * (e.g., polling, multi-step checks, or using third-party APIs).</p>
     *
     * <p>The class must have a public no-args constructor.</p>
     */
    Class<? extends Consumer<Object>> awaitConsumer() default NoConsumer.class;

    /**
     * Container annotation for repeatable {@link FutureLikeAwait} declarations.
     */
    @Inherited
    @Documented
    @Target(TYPE)
    @Retention(RUNTIME)
    @interface List {
        FutureLikeAwait[] value();
    }

    /**
     * Default marker used when no await strategy is configured.
     * Always throws {@link UnsupportedOperationException} if invoked.
     */
    final class NoConsumer implements Consumer<Object> {
        @Override
        public void accept(Object input) {
            throw new UnsupportedOperationException(
                    "No await strategy provided for @FutureLikeAwait. " +
                            "Specify either 'await' (expression) or 'awaitConsumer' (class)."
            );
        }
    }
}
