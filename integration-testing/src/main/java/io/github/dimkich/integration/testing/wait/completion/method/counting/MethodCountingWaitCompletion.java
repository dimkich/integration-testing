package io.github.dimkich.integration.testing.wait.completion.method.counting;

import io.github.dimkich.integration.testing.expression.ExpressionFactory;
import io.github.dimkich.integration.testing.expression.PointcutMatch;
import io.github.dimkich.integration.testing.expression.PointcutRegistry;
import io.github.dimkich.integration.testing.expression.PointcutSettings;
import io.github.dimkich.integration.testing.wait.completion.MethodCountingAwait;
import io.github.dimkich.integration.testing.wait.completion.WaitCompletion;
import lombok.SneakyThrows;
import net.bytebuddy.agent.builder.AgentBuilder;

import java.util.Collection;

/**
 * Implementation of the {@link WaitCompletion} strategy that tracks asynchronous
 * activity by counting "in-flight" method invocations.
 *
 * <p>This strategy defines a "task" as the duration of a specific method execution
 * (from entry to exit). It is particularly effective for fire-and-forget background
 * processing, worker loops, or event-driven systems where completion is tied to
 * the termination of a processing method.</p>
 *
 * <h3>How it works:</h3>
 * <ul>
 *   <li><b>Instrumentation:</b> During agent setup, it injects {@link MethodCountingAdvice}
 *       into methods matching the {@link MethodCountingAwait#pointcut()} expression.</li>
 *   <li><b>Tracking:</b> Every call to an instrumented method increments a global counter
 *       on entry and decrements it on exit (including exceptional exits).</li>
 *   <li><b>Synchronization:</b> The test execution blocks until the counter reaches
 *       zero, ensuring all intercepted methods have finished their execution.</li>
 * </ul>
 *
 * @see MethodCountingAwait
 * @see MethodCountingTracker
 * @see MethodCountingAdvice
 */
public class MethodCountingWaitCompletion implements WaitCompletion {

    /**
     * Configures the provided {@link AgentBuilder} with ByteBuddy transformations
     * for all specified {@link MethodCountingAwait} pointcuts.
     *
     * <p>For each configuration, this method:
     * <ol>
     *     <li>Compiles the <b>pointcut</b> expression via {@link ExpressionFactory}
     *         to resolve class and method matchers.</li>
     *     <li>Compiles the <b>when</b> expression into a runtime predicate for
     *         dynamic filtering (e.g., counting only calls with specific arguments).</li>
     *     <li>Registers the compiled settings in the {@link PointcutRegistry}.</li>
     *     <li>Applies the {@link MethodCountingAdvice} to the identified injection points.</li>
     * </ol></p>
     *
     * @param awaits       collection of await configurations describing which methods to track.
     * @param agentBuilder base {@link AgentBuilder} instance to extend with transformations.
     * @return the supplied {@link AgentBuilder} extended with method-counting instrumentation.
     * @throws IllegalArgumentException if the pointcut expression fails to resolve or lacks a method matcher.
     */
    public static AgentBuilder setUp(Collection<MethodCountingAwait> awaits, AgentBuilder agentBuilder) {
        for (MethodCountingAwait await : awaits) {
            PointcutMatch match = ExpressionFactory.createPointcutMatch(await.pointcut());
            PointcutSettings settings = PointcutRegistry.get(match.getPointcutId());

            settings.setWhen(ExpressionFactory.createInvokePredicate(await.when()));

            agentBuilder = match.apply(agentBuilder, MethodCountingAdvice.class);
        }
        return agentBuilder;
    }

    /**
     * Resets the internal {@link MethodCountingTracker} state.
     * <p>Clears the active task counter and the activity flag to ensure a clean state
     * before starting a new tracking cycle in a test.</p>
     */
    @Override
    public void start() {
        MethodCountingTracker.reset();
    }

    /**
     * Indicates whether any tracked method activity has been observed
     * since the last {@link #start()} or {@link #waitCompletion()} call.
     *
     * @return {@code true} if at least one tracked method was invoked; otherwise {@code false}.
     */
    @Override
    public boolean isAnyTaskStarted() {
        return MethodCountingTracker.isAnyActivity();
    }

    /**
     * Blocks the current thread until all tracked method activities are completed.
     * <p>This method delegates the blocking logic to {@link MethodCountingTracker#waitCompletion()},
     * which waits for the atomic counter of in-flight executions to reach zero.</p>
     *
     * @throws InterruptedException (wrapped in SneakyThrows) if the thread is interrupted while waiting.
     */
    @Override
    @SneakyThrows
    public void waitCompletion() {
        MethodCountingTracker.waitCompletion();
    }
}
