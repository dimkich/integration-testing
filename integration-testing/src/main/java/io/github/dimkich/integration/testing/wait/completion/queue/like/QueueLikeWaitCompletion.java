package io.github.dimkich.integration.testing.wait.completion.queue.like;

import io.github.dimkich.integration.testing.expression.ExpressionFactory;
import io.github.dimkich.integration.testing.expression.PointcutMatch;
import io.github.dimkich.integration.testing.expression.PointcutRegistry;
import io.github.dimkich.integration.testing.expression.PointcutSettings;
import io.github.dimkich.integration.testing.instrumentation.InstrumentationPlugin;
import io.github.dimkich.integration.testing.wait.completion.QueueLikeAwait;
import io.github.dimkich.integration.testing.wait.completion.WaitCompletion;
import io.github.dimkich.integration.testing.wait.completion.WaitCompletionProperties;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.agent.builder.AgentBuilder;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

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
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class QueueLikeWaitCompletion implements WaitCompletion, InstrumentationPlugin {

    private final WaitCompletionProperties properties;

    /**
     * Creates the ServiceLoader plugin instance. It only configures instrumentation;
     * the wait methods run on the Spring bean, which receives the settings through
     * the constructor with {@link WaitCompletionProperties}.
     */
    public QueueLikeWaitCompletion() {
        this(null);
    }

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
     * Resets the activity flag; service tracking itself starts automatically via advice when
     * matching types are instantiated.
     */
    @Override
    public void start() {
        QueueLikeTracker.resetActivity();
    }

    /**
     * Blocks the calling thread by polling the total pending task count across all
     * registered services until it reaches zero, then atomically takes the activity flag.
     *
     * <p>It uses a 1ms sleep interval between polls to minimize CPU overhead while
     * maintaining high responsiveness to task completion.</p>
     *
     * @return {@code true} if a service was registered or a non-empty queue was observed
     * @throws IllegalStateException if queues do not drain within the given timeout
     */
    @Override
    @SneakyThrows
    @SuppressWarnings("BusyWait")
    public boolean awaitAndCheckActivity() {
        long timeoutMs = properties.getTimeoutMs();
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        int count;
        while ((count = QueueLikeTracker.getCount(true)) > 0) {
            long remainingNanos = deadline - System.nanoTime();
            if (remainingNanos <= 0) {
                throw new IllegalStateException("Timed out waiting for queue-like tasks to drain after "
                        + timeoutMs + " ms, " + count + " pending task(s)");
            }
            Thread.sleep(1);
        }
        boolean activity = QueueLikeTracker.takeActivity();
        log.debug("All queue-like objects are empty, activity: {}", activity);
        return activity;
    }
}
