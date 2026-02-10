package io.github.dimkich.integration.testing.wait.completion.method.pair;

import io.github.dimkich.integration.testing.expression.ExpressionFactory;
import io.github.dimkich.integration.testing.expression.PointcutMatch;
import io.github.dimkich.integration.testing.expression.PointcutRegistry;
import io.github.dimkich.integration.testing.wait.completion.MethodPairAwait;
import io.github.dimkich.integration.testing.wait.completion.WaitCompletion;
import lombok.SneakyThrows;
import net.bytebuddy.agent.builder.AgentBuilder;

import java.util.Collection;

/**
 * Implementation of the {@link WaitCompletion} strategy that synchronizes test execution
 * based on pairs of "start" and "end" method invocations.
 *
 * <p>This strategy is specifically designed for complex asynchronous workflows where
 * a task's lifecycle spans across different methods (e.g., a request sent in one
 * component and a response processed in another). It uses ByteBuddy to inject
 * {@link MethodPairEnterAdvice} into start points and {@link MethodPairExitAdvice}
 * into end points, both sharing a single atomic counter via {@link MethodPairTracker}.</p>
 *
 * <h3>Instrumentation Workflow:</h3>
 * <ul>
 *   <li><b>Pairing:</b> For each {@link MethodPairAwait} annotation, two distinct pointcuts
 *       are resolved and linked to a single shared counter.</li>
 *   <li><b>Conditional Tracking:</b> Separate "when" conditions can be applied to both
 *       the start and end points of the pair.</li>
 *   <li><b>State Management:</b> The test thread blocks until the shared counter for
 *       all registered pairs returns to zero.</li>
 * </ul>
 *
 * @see MethodPairAwait
 * @see MethodPairTracker
 * @see MethodPairEnterAdvice
 * @see MethodPairExitAdvice
 */
public class MethodPairWaitCompletion implements WaitCompletion {

    /**
     * Configures the provided {@link AgentBuilder} with ByteBuddy transformations
     * for all specified start/end method pairs.
     *
     * <p>For each {@link MethodPairAwait} configuration, this method:
     * <ol>
     *     <li>Compiles the <b>startPointcut</b> and <b>endPointcut</b> expressions.</li>
     *     <li>Compiles the <b>startWhen</b> and <b>endWhen</b> runtime conditions.</li>
     *     <li>Registers the two pointcut IDs as a linked pair in the {@link MethodPairTracker}.</li>
     *     <li>Applies entry advice to the start method and exit advice to the end method.</li>
     * </ol></p>
     *
     * @param awaits       collection of await configurations describing the start/end methods.
     * @param agentBuilder base {@link AgentBuilder} instance to extend with transformations.
     * @return the supplied {@link AgentBuilder} instance with all pair transformations registered.
     * @throws IllegalArgumentException if a start or end pointcut fails to define a method matcher.
     */
    public static AgentBuilder setUp(Collection<MethodPairAwait> awaits, AgentBuilder agentBuilder) {
        for (MethodPairAwait await : awaits) {
            PointcutMatch startMatch = ExpressionFactory.createPointcutMatch(await.startPointcut());
            PointcutMatch endMatch = ExpressionFactory.createPointcutMatch(await.endPointcut());

            PointcutRegistry.get(startMatch.getPointcutId())
                    .setWhen(ExpressionFactory.createInvokePredicate(await.startWhen()));
            PointcutRegistry.get(endMatch.getPointcutId())
                    .setWhen(ExpressionFactory.createInvokePredicate(await.endWhen()));

            MethodPairTracker.registerPair(startMatch.getPointcutId(), endMatch.getPointcutId());

            agentBuilder = startMatch.apply(agentBuilder, MethodPairEnterAdvice.class);
            agentBuilder = endMatch.apply(agentBuilder, MethodPairExitAdvice.class);
        }
        return agentBuilder;
    }

    /**
     * Completely clears all registered method-pair configurations and their counters.
     * <p>This should be called during the cleanup phase of the test suite to
     * remove stale instrumentation rules.</p>
     */
    public static void tearDown() {
        MethodPairTracker.clear();
    }

    /**
     * Resets the {@link MethodPairTracker} state for the upcoming test.
     * <p>Sets all shared counters to zero and clears the activity flag while
     * maintaining the pointcut pair registrations.</p>
     */
    @Override
    public void start() {
        MethodPairTracker.reset();
    }

    /**
     * Indicates whether any tracked method pair activity has been detected
     * since the last {@link #start()} or {@link #waitCompletion()} call.
     *
     * @return {@code true} if at least one start method of a pair was invoked; otherwise {@code false}.
     */
    @Override
    public boolean isAnyTaskStarted() {
        return MethodPairTracker.isAnyActivity();
    }

    /**
     * Blocks the current thread until all tracked method-pair activities are completed.
     * <p>Delegates the waiting logic to {@link MethodPairTracker#waitCompletion()},
     * which blocks until the sum of all paired counters reaches zero.</p>
     *
     * @throws InterruptedException (wrapped in SneakyThrows) if the thread is interrupted while waiting.
     */
    @Override
    @SneakyThrows
    public void waitCompletion() {
        MethodPairTracker.waitCompletion();
    }
}
