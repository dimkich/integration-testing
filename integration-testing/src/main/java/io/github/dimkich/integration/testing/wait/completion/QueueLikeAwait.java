package io.github.dimkich.integration.testing.wait.completion;

import io.github.dimkich.integration.testing.IntegrationTesting;

import java.lang.annotation.*;
import java.util.function.Function;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Defines an instrumentation strategy to track services that expose a "pending tasks" count.
 *
 * <p>This strategy is unique because it does not rely on event counters (increment/decrement).
 * Instead, it <b>polls</b> the state of registered objects. When a test needs to wait,
 * the framework repeatedly calls a "size" function on all captured instances until the
 * total sum of tasks across all services reaches zero.</p>
 *
 * <h3>Key Characteristics:</h3>
 * <ul>
 *   <li><b>State Polling:</b> Ideal for classes that already have methods like {@code size()},
 *       {@code getQueueSize()}, or {@code getActiveCount()}.</li>
 *   <li><b>Leak Prevention:</b> Uses {@link java.lang.ref.WeakReference} to ensure that
 *       tracking does not prevent garbage collection of the monitored services.</li>
 *   <li><b>Flexibility:</b> Can track instances created via constructors or returned from
 *       factory methods.</li>
 * </ul>
 *
 * <h3>Usage Examples:</h3>
 *
 * <p><b>1. Real-world (QuickFix/J / MINA):</b> Wait until the event handling queue is empty.
 * <pre>{@code
 * @QueueLikeAwait(
 *     pointcut = "t.inherits('quickfix.mina.EventHandlingStrategy') && m.isConstructor()",
 *     size = "o.call('getQueueSize').asInt()"
 * )
 * }</pre></p>
 *
 * <p><b>2. Custom Task Tracker:</b> Poll a method that returns a list and check its size.
 * <pre>{@code
 * @QueueLikeAwait(
 *     pointcut = "t.name('com.app.MyWorker') && m.isConstructor()",
 *     size = "o.call('getPendingTasks').asList().size()"
 * )
 * }</pre></p>
 *
 * <p><b>3. Complex Filtering:</b> Track only specific implementations marked with an annotation.
 * <pre>{@code
 * @QueueLikeAwait(
 *     pointcut = "t.packageStartsWith('com.app') && t.ann('com.app.Monitorable') && m.isConstructor()",
 *     when = "o.get().getClass().isAnnotationPresent(com.app.Monitorable.class)",
 *     size = "o.call('taskCount').asInt()"
 * )
 * }</pre></p>
 *
 * <p><b>4. Class-based Strategy:</b> Use a dedicated Java class for complex size calculation.
 * <pre>{@code
 * @QueueLikeAwait(
 *     pointcut = "t.name('com.app.ComplexService') && m.name('create')",
 *     sizeFunction = MyCustomSizeFunction.class
 * )
 * }</pre></p>
 *
 * @see io.github.dimkich.integration.testing.wait.completion.queue.like.QueueLikeWaitCompletion
 * @see io.github.dimkich.integration.testing.wait.completion.queue.like.QueueLikeTracker
 */
@Inherited
@Documented
@IntegrationTesting
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(QueueLikeAwait.List.class)
public @interface QueueLikeAwait {

    /**
     * A DSL expression that identifies which classes or factory methods produce the
     * services to be monitored.
     *
     * <p>If the method matcher ({@code m.name}) is omitted, all constructors of the
     * matched type will be instrumented.</p>
     *
     * @return DSL selector for the service injection point.
     */
    String pointcut();

    /**
     * A runtime DSL expression evaluated when a service is created to decide
     * if it should be registered for polling.
     *
     * <p>Available variables: {@code o} (the created object), {@code a} (arguments).</p>
     * @return a DSL condition string (defaults to {@code "true"}).
     */
    String when() default "true";

    /**
     * A DSL expression used to retrieve the "pending task count" from the service instance.
     *
     * <p>The expression must return an {@code Integer}. This is an alternative to {@link #sizeFunction()}.</p>
     * <p>Available variable: {@code o} (the service instance as {@code ObjectWrapper}).</p>
     * @example {@code size = "o.call('getQueue').call('size').asInt()"}
     */
    String size() default "";

    /**
     * A custom {@link Function} implementation to calculate the task count.
     *
     * <p>Use this for logic too complex for a single DSL string. The class must
     * have a public no-args constructor.</p>
     */
    Class<? extends Function<Object, Integer>> sizeFunction() default NoFunction.class;

    /**
     * Container annotation for repeatable {@link QueueLikeAwait} declarations.
     */
    @Inherited
    @Documented
    @Target(TYPE)
    @Retention(RUNTIME)
    @interface List {
        QueueLikeAwait[] value();
    }

    /**
     * Default marker function. Throws an exception if no size strategy is provided.
     */
    final class NoFunction implements Function<Object, Integer> {
        @Override
        public Integer apply(Object input) {
            throw new UnsupportedOperationException(
                    "No task counting strategy provided for @QueueLikeAwait. " +
                            "Specify either 'size' (expression) or 'sizeFunction' (class)."
            );
        }
    }
}
