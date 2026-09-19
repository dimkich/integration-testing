package io.github.dimkich.integration.testing.wait.completion.queue.like;

import io.github.dimkich.integration.testing.expression.ExpressionFactory;
import io.github.dimkich.integration.testing.expression.PointcutMatch;
import io.github.dimkich.integration.testing.expression.PointcutRegistry;
import io.github.dimkich.integration.testing.expression.PointcutSettings;
import io.github.dimkich.integration.testing.instrumentation.InstrumentationPlugin;
import io.github.dimkich.integration.testing.wait.completion.QueueLikeAwait;
import io.github.dimkich.integration.testing.wait.completion.WaitCompletion;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.agent.builder.AgentBuilder;

import java.util.Collection;

import static org.springframework.core.annotation.AnnotatedElementUtils.findMergedRepeatableAnnotations;

/**
 * Implementation of the {@link WaitCompletion} strategy that synchronizes tests by polling
 * the state of "queue-like" services.
 *
 * <p>Unlike event-driven strategies (which increment/decrement counters), this strategy
 * actively queries the target objects for their current workload. It is specifically designed
 * for services that maintain internal buffers, task queues, or thread pools where the
 * completion of work is defined by the queue becoming empty.</p>
 *
 * <h3>Workflow:</h3>
 * <ul>
 *   <li><b>Discovery:</b> ByteBuddy instruments constructors or factory methods to capture
 *       instances of services matching the {@link QueueLikeAwait#pointcut()}.</li>
 *   <li><b>Registration:</b> Captured instances are registered in {@link QueueLikeTracker}
 *       along with a function to extract their "size" or "pending count".</li>
 *   <li><b>Polling:</b> During the wait phase, the framework repeatedly invokes the
 *       extraction function until the aggregate count of all tracked services reaches zero.</li>
 * </ul>
 *
 * @see QueueLikeAwait
 * @see QueueLikeTracker
 * @see QueueLikeAdvice
 */
@Slf4j
public class QueueLikeWaitCompletion implements WaitCompletion, InstrumentationPlugin {

    @Override
    public boolean isApplicable(Class<?> testClass) {
        return !findMergedRepeatableAnnotations(testClass, QueueLikeAwait.class).isEmpty();
    }

    @Override
    public AgentBuilder configureBuilder(Class<?> testClass, AgentBuilder builder) throws ReflectiveOperationException {
        Collection<QueueLikeAwait> awaits = findMergedRepeatableAnnotations(testClass, QueueLikeAwait.class);
        for (QueueLikeAwait await : awaits) {
            PointcutMatch match = ExpressionFactory.createPointcutMatch(await.pointcut());
            PointcutSettings settings = PointcutRegistry.get(match.getPointcutId());

            settings.setWhen(ExpressionFactory.createInvokePredicate(await.when()));

            if (!await.size().isEmpty()) {
                settings.setCount(ExpressionFactory.createObjectFunction(await.size(), Integer.class));
            } else {
                settings.setCount(await.sizeFunction().getConstructor().newInstance());
            }
            builder = match.apply(builder, QueueLikeAdvice.class);
        }
        return builder;
    }

    @Override
    public void cleanup() {
        QueueLikeTracker.clear();
    }

    /**
     * Start is a no-op for this strategy as tracking begins automatically
     * via Advice when matching types are instantiated.
     */
    @Override
    public void start() {
    }

    /**
     * Indicates whether any tracked service currently reports a non-zero task count.
     *
     * @return {@code true} if the aggregate count of pending tasks is greater than 0.
     */
    @Override
    public boolean isAnyTaskStarted() {
        return QueueLikeTracker.getCount(false) > 0;
    }

    /**
     * Blocks the calling thread by polling the total pending task count across all
     * registered services until it reaches zero.
     *
     * <p>It uses a 1ms sleep interval between polls to minimize CPU overhead while
     * maintaining high responsiveness to task completion.</p>
     */
    @Override
    @SneakyThrows
    public void waitCompletion() {
        while (QueueLikeTracker.getCount(true) > 0) {
            Thread.sleep(1);
        }
        log.debug("All queue-like objects are empty");
    }
}
