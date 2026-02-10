package io.github.dimkich.integration.testing.wait.completion;

import io.github.dimkich.integration.testing.IntegrationTesting;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Defines a strategy to track asynchronous tasks that span across multiple method calls
 * by linking a "start" method and an "end" method into a single logical pair.
 *
 * <p>This annotation is essential for complex asynchronous flows where a task's lifecycle
 * is not contained within a single method execution (like {@link MethodCountingAwait})
 * or a single return object (like {@link FutureLikeAwait}). It maintains a <b>shared counter</b>
 * that increments when the start method is hit and decrements when the corresponding
 * end method is invoked.</p>
 *
 * <h3>Key Characteristics:</h3>
 * <ul>
 *   <li><b>Shared Counter Logic:</b> A single atomic counter is shared between the start
 *       and end pointcuts. The framework ensures the test blocks until the
 *       net count of in-flight tasks returns to zero.</li>
 *   <li><b>Cross-Component Tracking:</b> Allows tracking tasks that start in one class
 *       (e.g., a message listener) and finish in another (e.g., a business logic processor).</li>
 *   <li><b>Dual Filtering:</b> Supports independent {@code when} conditions for both
 *       the start and end points of the task.</li>
 * </ul>
 *
 * <h3>Usage Examples:</h3>
 *
 * <p><b>1. Real-world (QuickFix/J):</b> Wait for a message that is received in the MINA
 * event strategy and processed in the Session logic.
 * <pre>{@code
 * @MethodPairAwait(
 *     startPointcut = "t.inherits('quickfix.mina.EventHandlingStrategy') && m.name('onMessage')",
 *     endPointcut = "t.name('quickfix.Session') && m.name('next')"
 * )
 * }</pre></p>
 *
 * <p><b>2. Request-Response pattern:</b>
 * <pre>{@code
 * @MethodPairAwait(
 *     startPointcut = "t.name('com.app.HttpClient') && m.name('sendRequest')",
 *     startWhen = "a.arg(0).asString().contains('/api/data')",
 *     endPointcut = "t.name('com.app.ResponseHandler') && m.name('handleResponse')",
 *     endWhen = "o.field('statusCode').asInt() == 200"
 * )
 * }</pre></p>
 *
 * @see io.github.dimkich.integration.testing.wait.completion.method.pair.MethodPairWaitCompletion
 * @see io.github.dimkich.integration.testing.wait.completion.method.pair.MethodPairTracker
 */
@Inherited
@Documented
@IntegrationTesting
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(MethodPairAwait.List.class)
public @interface MethodPairAwait {

    /**
     * A DSL expression that identifies the method(s) marking the <b>start</b> of a tracked activity.
     *
     * <p>When this method is entered, the shared counter for the pair is incremented.
     * Must include a method matcher (e.g., {@code m.name(...)}) to be valid.</p>
     *
     * @return DSL selector for the start point (e.g., {@code "t.name('Listener') && m.name('onEvent')"}).
     */
    String startPointcut();

    /**
     * A runtime DSL expression evaluated at the <b>start</b> point to determine if the task should be tracked.
     *
     * <p>Available variables: {@code o} (target object), {@code a} (arguments).</p>
     * @return a DSL condition string (defaults to {@code "true"}).
     */
    String startWhen() default "true";

    /**
     * A DSL expression that identifies the method(s) marking the <b>end</b> of a tracked activity.
     *
     * <p>When this method finishes (even with an exception), the shared counter for the pair
     * is decremented.</p>
     *
     * @return DSL selector for the end point (e.g., {@code "t.name('Processor') && m.name('cleanup')"}).
     */
    String endPointcut();

    /**
     * A runtime DSL expression evaluated at the <b>end</b> point to determine if the
     * counter should be decremented.
     *
     * <p>Ensure this logic correlates with {@link #startWhen()} to prevent counter leaks or
     * negative counts.</p>
     *
     * @return a DSL condition string (defaults to {@code "true"}).
     */
    String endWhen() default "true";

    /**
     * Container annotation for repeatable {@link MethodPairAwait} declarations.
     */
    @Inherited
    @Documented
    @Target(TYPE)
    @Retention(RUNTIME)
    @interface List {
        MethodPairAwait[] value();
    }
}
