package io.github.dimkich.integration.testing.expression;

import lombok.Getter;
import lombok.Setter;

import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A runtime configuration container that defines the behavioral logic for a specific pointcut.
 *
 * <p>This class acts as a bridge between the static instrumentation definitions (annotations)
 * and the dynamic execution logic. It holds functional delegates created via
 * {@link ExpressionFactory} that are invoked by ByteBuddy Advice to control the
 * instrumentation lifecycle.</p>
 *
 * <h3>Functional Roles:</h3>
 * <ul>
 *   <li><b>When (Filter):</b> Determines if a specific method call or object instance
 *       should be tracked. Used by all strategies to filter activity by arguments or state.</li>
 *   <li><b>Await (Action):</b> Defines the blocking logic for {@code FutureLike} strategies
 *       (e.g., calling {@code join()} or {@code get()}).</li>
 *   <li><b>Count (Polling):</b> Defines the state-extraction logic for {@code QueueLike}
 *       strategies (e.g., querying the current size of an internal queue).</li>
 * </ul>
 *
 * <p>Uses the "Null Object" pattern for default values to ensure thread-safe execution
 * without explicit null checks in performance-critical Advice code.</p>
 */
@Getter
@Setter
public class PointcutSettings {

    /**
     * Default predicate used when no 'when' condition is specified. Always returns {@code true}.
     */
    private static final BiPredicate<Object, Object[]> ALWAYS_TRUE_WHEN = (o, args) -> true;

    /**
     * Default no-op consumer used when no 'await' action is defined.
     */
    private static final Consumer<Object> EMPTY_AWAIT = o -> {
    };

    /**
     * Default function used when no 'count' polling is defined. Always returns {@code 0}.
     */
    private static final Function<Object, Integer> ZERO_COUNT = o -> 0;

    /**
     * The condition to be evaluated during method entry or object creation.
     * <p>Maps to the {@code when()} attribute in {@code @FutureLikeAwait},
     * {@code @MethodCountingAwait}, etc.</p>
     */
    private BiPredicate<Object, Object[]> when = ALWAYS_TRUE_WHEN;

    /**
     * The blocking action to perform on a tracked instance.
     * <p>Used by {@code FutureLikeWaitCompletion} to synchronize with asynchronous tasks.</p>
     */
    private Consumer<Object> await = EMPTY_AWAIT;

    /**
     * The function used to poll the current number of pending tasks from a service.
     * <p>Used by {@code QueueLikeWaitCompletion} to calculate the aggregate workload.</p>
     */
    private Function<Object, Integer> count = ZERO_COUNT;

    /**
     * Evaluates the 'when' condition against the intercepted object and its arguments.
     *
     * <p>This method includes a performance optimization that skips the predicate
     * execution if the condition is set to the default "always true" state.</p>
     *
     * @param o    the intercepted object instance ('this' or returned object).
     * @param args the arguments passed to the intercepted constructor or method.
     * @return {@code true} if the instrumentation logic should proceed for this specific call.
     */
    public boolean checkWhen(Object o, Object[] args) {
        return when == ALWAYS_TRUE_WHEN || when.test(o, args);
    }
}
