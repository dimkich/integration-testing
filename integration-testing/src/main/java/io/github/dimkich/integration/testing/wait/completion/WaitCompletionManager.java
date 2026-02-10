package io.github.dimkich.integration.testing.wait.completion;

import io.github.dimkich.integration.testing.wait.completion.future.like.FutureLikeWaitCompletion;
import io.github.dimkich.integration.testing.wait.completion.method.counting.MethodCountingWaitCompletion;
import io.github.dimkich.integration.testing.wait.completion.method.pair.MethodPairWaitCompletion;
import io.github.dimkich.integration.testing.wait.completion.queue.like.QueueLikeWaitCompletion;
import net.bytebuddy.agent.builder.AgentBuilder;

import static org.springframework.core.annotation.AnnotatedElementUtils.findMergedRepeatableAnnotations;

/**
 * The central orchestrator for the Wait-Completion subsystem.
 *
 * <p>This manager is responsible for the lifecycle of asynchronous task tracking
 * during integration tests. It acts as a bridge between the test class
 * configuration (via annotations) and the ByteBuddy instrumentation engine.</p>
 *
 * <h3>Core Responsibilities:</h3>
 * <ul>
 *   <li><b>Bootstrap:</b> Scans the test class for {@code @FutureLikeAwait},
 *       {@code @MethodCountingAwait}, {@code @MethodPairAwait}, and {@code @QueueLikeAwait}
 *       annotations and configures the {@link AgentBuilder} accordingly.</li>
 *   <li><b>Multi-Strategy Support:</b> Aggregates multiple instrumentation techniques
 *       to handle different types of asynchronous activity in a single test.</li>
 *   <li><b>Lifecycle Management:</b> Provides a {@code tearDown} mechanism to
 *       clean up global registries and trackers between test runs.</li>
 * </ul>
 *
 * @see FutureLikeWaitCompletion
 * @see MethodCountingWaitCompletion
 * @see MethodPairWaitCompletion
 * @see QueueLikeWaitCompletion
 */
public class WaitCompletionManager {

    /**
     * Configures the ByteBuddy agent based on the wait-completion annotations
     * present on the provided test class.
     *
     * <p>This method performs a "merged" search for repeatable annotations,
     * meaning it respects inheritance and multiple declarations. It sequentially
     * applies transformations for each supported strategy to the {@link AgentBuilder}.</p>
     *
     * @param testClass the test class to scan for instrumentation rules.
     * @param builder   the base {@link AgentBuilder} to be extended.
     * @return an instrumented {@code AgentBuilder} with all registered wait-completion rules.
     * @throws ReflectiveOperationException if custom consumers or functions defined in
     *                                      annotations cannot be instantiated.
     */
    public static AgentBuilder setUp(Class<?> testClass, AgentBuilder builder) throws ReflectiveOperationException {
        builder = FutureLikeWaitCompletion.setUp(
                findMergedRepeatableAnnotations(testClass, FutureLikeAwait.class), builder);
        builder = MethodCountingWaitCompletion.setUp(
                findMergedRepeatableAnnotations(testClass, MethodCountingAwait.class), builder);
        builder = MethodPairWaitCompletion.setUp(
                findMergedRepeatableAnnotations(testClass, MethodPairAwait.class), builder);
        return QueueLikeWaitCompletion.setUp(
                findMergedRepeatableAnnotations(testClass, QueueLikeAwait.class), builder);
    }

    /**
     * Performs a global cleanup of the wait-completion trackers.
     *
     * <p>Should be invoked after test execution (e.g., in {@code @AfterAll} or
     * during agent shutdown) to clear registries and prevent memory leaks or
     * cross-test interference, especially for strategies that maintain stateful
     * maps like {@code MethodPair} and {@code QueueLike}.</p>
     */
    public static void tearDown() {
        MethodPairWaitCompletion.tearDown();
        QueueLikeWaitCompletion.tearDown();
    }
}
