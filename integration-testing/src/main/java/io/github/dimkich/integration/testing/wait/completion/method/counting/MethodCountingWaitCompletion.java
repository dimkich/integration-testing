package io.github.dimkich.integration.testing.wait.completion.method.counting;

import io.github.dimkich.integration.testing.expression.ExpressionFactory;
import io.github.dimkich.integration.testing.expression.PointcutMatch;
import io.github.dimkich.integration.testing.expression.PointcutRegistry;
import io.github.dimkich.integration.testing.expression.PointcutSettings;
import io.github.dimkich.integration.testing.instrumentation.InstrumentationPlugin;
import io.github.dimkich.integration.testing.wait.completion.MethodCountingAwait;
import io.github.dimkich.integration.testing.wait.completion.WaitCompletion;
import lombok.SneakyThrows;
import net.bytebuddy.agent.builder.AgentBuilder;

import java.util.Collection;

import static org.springframework.core.annotation.AnnotatedElementUtils.findMergedRepeatableAnnotations;

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
 *       on entry and decrements it on exit (including exceptional exits). It also marks
 *       the activity flag, so work that finished before the wait is still reported.</li>
 *   <li><b>Synchronization:</b> The coordinator step blocks until the counter reaches
 *       zero and takes the activity flag atomically.</li>
 * </ul>
 *
 * @see MethodCountingAwait
 * @see MethodCountingTracker
 * @see MethodCountingAdvice
 */
public class MethodCountingWaitCompletion implements WaitCompletion, InstrumentationPlugin {

    @Override
    public boolean isApplicable(Class<?> testClass) {
        return !findMergedRepeatableAnnotations(testClass, MethodCountingAwait.class).isEmpty();
    }

    @Override
    public AgentBuilder configureBuilder(Class<?> testClass, AgentBuilder builder) {
        Collection<MethodCountingAwait> awaits = findMergedRepeatableAnnotations(testClass, MethodCountingAwait.class);
        for (MethodCountingAwait await : awaits) {
            PointcutMatch match = ExpressionFactory.createPointcutMatch(await.pointcut());
            PointcutSettings settings = PointcutRegistry.get(match.getPointcutId());

            settings.setWhen(ExpressionFactory.createInvokePredicate(await.when()));

            builder = match.apply(builder, MethodCountingAdvice.class);
        }
        return builder;
    }

    /**
     * Resets the internal {@link MethodCountingTracker} state: the active task counter and
     * the activity flag.
     */
    @Override
    public void start() {
        MethodCountingTracker.reset();
    }

    /**
     * Blocks until all tracked method activities are completed and returns whether any
     * tracked method was invoked since {@link #start()} or the previous call.
     *
     * @return {@code true} if at least one tracked method was invoked during the window
     */
    @Override
    @SneakyThrows
    public boolean awaitAndCheckActivity() {
        return MethodCountingTracker.awaitAndCheckActivity();
    }
}
