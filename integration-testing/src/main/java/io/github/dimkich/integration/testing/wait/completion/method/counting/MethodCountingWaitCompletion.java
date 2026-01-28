package io.github.dimkich.integration.testing.wait.completion.method.counting;

import io.github.dimkich.integration.testing.util.ByteBuddyUtils;
import io.github.dimkich.integration.testing.wait.completion.ClassPredicate;
import io.github.dimkich.integration.testing.wait.completion.MethodCountingAwait;
import io.github.dimkich.integration.testing.wait.completion.Pointcut;
import io.github.dimkich.integration.testing.wait.completion.WaitCompletion;
import io.github.dimkich.integration.testing.wait.completion.parser.ByteBuddySelectorParser;
import io.github.dimkich.integration.testing.wait.completion.parser.ByteBuddySelectorResult;
import lombok.SneakyThrows;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;

import java.util.Collection;

/**
 * {@link WaitCompletion} implementation that tracks method invocations using ByteBuddy
 * and {@link MethodCountingTracker} to determine when all configured activities are complete.
 */
public class MethodCountingWaitCompletion implements WaitCompletion {

    /**
     * Configures the provided {@link AgentBuilder} with ByteBuddy transformations for all
     * specified {@link MethodCountingAwait} pointcuts.
     *
     * @param awaits       collection of await configurations describing which methods to track
     * @param agentBuilder agent builder to which the transformations should be applied
     * @return the same {@link AgentBuilder} instance with all transformations registered
     * @throws IllegalArgumentException if a {@link MethodCountingAwait} does not define a method matcher
     */
    public static AgentBuilder setUp(Collection<MethodCountingAwait> awaits, AgentBuilder agentBuilder) {
        MethodCountingTracker.setClassFilter(new ClassPredicate());
        for (MethodCountingAwait await : awaits) {
            ByteBuddySelectorResult selector = ByteBuddySelectorParser.parse(await.pointcut());
            if (selector.getMethodMatcher() == null) {
                throw new IllegalArgumentException("MethodCountingAwait must have a method matcher");
            }
            ClassPredicate.put(await.pointcut(), selector.getTypeFilter());

            agentBuilder = agentBuilder
                    .type(selector.getTypeMatcher())
                    .transform((builder, type, classLoader, module, domain) ->
                            builder.visit(Advice.withCustomMapping().bind(Pointcut.class, await.pointcut())
                                            .to(MethodCountingAdvice.class).on(selector.getMethodMatcher()))
                                    .visit(ByteBuddyUtils.getParameterWritingVisitorWrapper().apply(type))
                    );
        }
        return agentBuilder;
    }

    /**
     * Clears all registered class filters and pointcuts for method counting.
     * Should be called when method-counting based waiting is no longer needed.
     */
    public static void tearDown() {
        ClassPredicate.clear();
    }

    /**
     * Resets the internal {@link MethodCountingTracker} state at the beginning of a wait sequence.
     */
    @Override
    public void start() {
        MethodCountingTracker.reset();
    }

    /**
     * Indicates whether any tracked method activity has been observed.
     *
     * @return {@code true} if at least one tracked method was invoked, otherwise {@code false}
     */
    @Override
    public boolean isAnyTaskStarted() {
        return MethodCountingTracker.isAnyActivity();
    }

    /**
     * Blocks until all tracked method activities are completed.
     */
    @Override
    @SneakyThrows
    public void waitCompletion() {
        MethodCountingTracker.waitCompletion();
    }
}
