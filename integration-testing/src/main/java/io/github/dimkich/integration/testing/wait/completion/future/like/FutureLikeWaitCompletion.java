package io.github.dimkich.integration.testing.wait.completion.future.like;

import io.github.dimkich.integration.testing.expression.ExpressionFactory;
import io.github.dimkich.integration.testing.expression.PointcutMatch;
import io.github.dimkich.integration.testing.expression.PointcutRegistry;
import io.github.dimkich.integration.testing.expression.PointcutSettings;
import io.github.dimkich.integration.testing.instrumentation.InstrumentationPlugin;
import io.github.dimkich.integration.testing.wait.completion.FutureLikeAwait;
import io.github.dimkich.integration.testing.wait.completion.WaitCompletion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.agent.builder.AgentBuilder;

import java.util.Collection;

import static org.springframework.core.annotation.AnnotatedElementUtils.findMergedRepeatableAnnotations;

/**
 * Implementation of {@link WaitCompletion} that tracks and synchronizes with "future-like"
 * asynchronous tasks (e.g., {@code CompletableFuture}, {@code Thread}, or custom task objects).
 *
 * <p>A "future-like" task is any object created via a constructor or factory method that
 * matches a {@link FutureLikeAwait} pointcut. This strategy is ideal for cases where
 * the application code returns a handle to a background computation that needs to be
 * awaited (e.g., calling {@code .join()} or {@code .get()}) before the test can proceed.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class FutureLikeWaitCompletion implements WaitCompletion, InstrumentationPlugin {

    @Override
    public boolean isApplicable(Class<?> testClass) {
        return !findMergedRepeatableAnnotations(testClass, FutureLikeAwait.class).isEmpty();
    }

    @Override
    public AgentBuilder configureBuilder(Class<?> testClass, AgentBuilder builder) throws ReflectiveOperationException {
        Collection<FutureLikeAwait> awaits = findMergedRepeatableAnnotations(testClass, FutureLikeAwait.class);
        for (FutureLikeAwait await : awaits) {
            PointcutMatch match = ExpressionFactory.createPointcutMatch(await.pointcut());
            PointcutSettings settings = PointcutRegistry.get(match.getPointcutId());

            settings.setWhen(ExpressionFactory.createInvokePredicate(await.when()));
            if (!await.await().isEmpty()) {
                settings.setAwait(ExpressionFactory.createObjectConsumer(await.await()));
            } else {
                settings.setAwait(await.awaitConsumer().getConstructor().newInstance());
            }
            builder = match.apply(builder, FutureLikeAdvice.class);
        }
        return builder;
    }

    /**
     * Resets the {@link FutureLikeTracker}: pending tasks and the activity flag are cleared.
     */
    @Override
    public void start() {
        FutureLikeTracker.reset();
    }

    /**
     * Blocks the calling thread until all tracked tasks are completed (including tasks
     * registered while waiting) and returns whether any task was captured since
     * {@link #start()} or the previous call.
     *
     * @return {@code true} if at least one task was registered during the window
     */
    @Override
    public boolean awaitAndCheckActivity() {
        return FutureLikeTracker.awaitAndCheckActivity();
    }
}
