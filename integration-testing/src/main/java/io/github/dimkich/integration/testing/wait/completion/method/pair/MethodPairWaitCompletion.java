package io.github.dimkich.integration.testing.wait.completion.method.pair;

import io.github.dimkich.integration.testing.util.ByteBuddyUtils;
import io.github.dimkich.integration.testing.wait.completion.ClassPredicate;
import io.github.dimkich.integration.testing.wait.completion.MethodPairAwait;
import io.github.dimkich.integration.testing.wait.completion.Pointcut;
import io.github.dimkich.integration.testing.wait.completion.WaitCompletion;
import io.github.dimkich.integration.testing.wait.completion.parser.ByteBuddySelectorParser;
import io.github.dimkich.integration.testing.wait.completion.parser.ByteBuddySelectorResult;
import lombok.SneakyThrows;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;

import java.util.Collection;

/**
 * {@link WaitCompletion} implementation that waits for completion based on
 * entering and exiting method pairs configured via {@link MethodPairAwait}.
 * <p>
 * This class installs Byte Buddy instrumentation that tracks matched
 * "enter" and "exit" methods using {@link MethodPairTracker} so tests
 * can block until all registered method pairs have completed.
 */
public class MethodPairWaitCompletion implements WaitCompletion {

    /**
     * Configures the provided {@link AgentBuilder} with ByteBuddy transformations for all
     * specified {@link MethodPairAwait} start/end pointcuts.
     * <p>
     * For each await definition this method:
     * <ul>
     *     <li>Parses the {@linkplain MethodPairAwait#startPointcut() start} and
     *     {@linkplain MethodPairAwait#endPointcut() end} selectors.</li>
     *     <li>Registers corresponding type filters in {@link ClassPredicate}.</li>
     *     <li>Registers method-pair tracking in {@link MethodPairTracker}.</li>
     *     <li>Installs {@link MethodPairEnterAdvice} and {@link MethodPairExitAdvice} on
     *     matching methods and enables parameter capturing via {@link ByteBuddyUtils}.</li>
     * </ul>
     *
     * @param awaits       collection of await configurations describing the start/end methods to track
     * @param agentBuilder agent builder to which the transformations should be applied
     * @return the same {@link AgentBuilder} instance with all transformations registered
     * @throws IllegalArgumentException if a start or end pointcut does not define a method matcher
     */
    public static AgentBuilder setUp(Collection<MethodPairAwait> awaits, AgentBuilder agentBuilder) {
        MethodPairTracker.setClassFilter(new ClassPredicate());
        for (MethodPairAwait await : awaits) {
            ByteBuddySelectorResult startSelector = ByteBuddySelectorParser.parse(await.startPointcut());
            if (startSelector.getMethodMatcher() == null) {
                throw new IllegalArgumentException("MethodPairAwait.startPointcut must have a method matcher");
            }
            ClassPredicate.put(await.startPointcut(), startSelector.getTypeFilter());
            ByteBuddySelectorResult endSelector = ByteBuddySelectorParser.parse(await.endPointcut());
            if (endSelector.getMethodMatcher() == null) {
                throw new IllegalArgumentException("MethodPairAwait.endPointcut must have a method matcher");
            }
            ClassPredicate.put(await.endPointcut(), endSelector.getTypeFilter());
            MethodPairTracker.register(await.startPointcut(), await.endPointcut());
            agentBuilder = agentBuilder
                    .type(startSelector.getTypeMatcher())
                    .transform((builder, type, classLoader, module, domain) ->
                            builder.visit(Advice.withCustomMapping().bind(Pointcut.class, await.startPointcut())
                                            .to(MethodPairEnterAdvice.class).on(startSelector.getMethodMatcher()))
                                    .visit(ByteBuddyUtils.getParameterWritingVisitorWrapper().apply(type))
                    );
            agentBuilder = agentBuilder
                    .type(endSelector.getTypeMatcher())
                    .transform((builder, type, classLoader, module, domain) ->
                            builder.visit(Advice.withCustomMapping().bind(Pointcut.class, await.endPointcut())
                                            .to(MethodPairExitAdvice.class).on(endSelector.getMethodMatcher()))
                                    .visit(ByteBuddyUtils.getParameterWritingVisitorWrapper().apply(type))
                    );

        }
        return agentBuilder;
    }

    /**
     * Clears all registered class filters and method-pair configurations.
     * <p>
     * Should be called when method-pair based waiting is no longer needed.
     */
    public static void tearDown() {
        ClassPredicate.clear();
        MethodPairTracker.clear();
    }

    /**
     * Resets the internal {@link MethodPairTracker} state at the beginning of a wait sequence.
     */
    @Override
    public void start() {
        MethodPairTracker.reset();
    }

    /**
     * Indicates whether any tracked method pair activity has been observed.
     *
     * @return {@code true} if at least one configured method pair has started, otherwise {@code false}
     */
    @Override
    public boolean isAnyTaskStarted() {
        return MethodPairTracker.isAnyActivity();
    }

    /**
     * Blocks until all tracked method-pair activities are completed.
     */
    @Override
    @SneakyThrows
    public void waitCompletion() {
        MethodPairTracker.waitCompletion();
    }
}
