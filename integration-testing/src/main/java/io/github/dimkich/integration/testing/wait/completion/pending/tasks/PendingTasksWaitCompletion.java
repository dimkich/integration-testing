package io.github.dimkich.integration.testing.wait.completion.pending.tasks;

import eu.ciechanowiec.sneakyfun.SneakyFunction;
import io.github.dimkich.integration.testing.util.ByteBuddyUtils;
import io.github.dimkich.integration.testing.wait.completion.ClassPredicate;
import io.github.dimkich.integration.testing.wait.completion.PendingTasksAwait;
import io.github.dimkich.integration.testing.wait.completion.Pointcut;
import io.github.dimkich.integration.testing.wait.completion.WaitCompletion;
import io.github.dimkich.integration.testing.wait.completion.parser.ByteBuddySelectorParser;
import io.github.dimkich.integration.testing.wait.completion.parser.ByteBuddySelectorResult;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import java.util.Collection;

/**
 * {@link WaitCompletion} implementation that tracks services exposing a notion of
 * "pending tasks" and blocks until all such tasks across tracked instances are
 * completed.
 * <p>
 * Services are matched via {@link PendingTasksAwait} pointcuts. Byte Buddy
 * instruments constructors or factory methods of matching types so that created
 * instances are registered with {@link PendingTasksTracker}. Each service
 * provides a count of its pending tasks via {@link PendingTasksAwait#countPendingTasksMethod()}
 * or {@link PendingTasksAwait#countPendingTasksFunction()}. {@link #waitCompletion()}
 * polls the total count until it reaches zero.
 */
@Slf4j
public class PendingTasksWaitCompletion implements WaitCompletion {

    /**
     * Configures Byte Buddy instrumentation for all {@link PendingTasksAwait} definitions.
     * <p>
     * For every await definition this method:
     * <ul>
     *     <li>Resolves the count function via {@link PendingTasksAwait#countPendingTasksFunction()}
     *     or {@link PendingTasksAwait#countPendingTasksMethod()} (reflection-based fallback).</li>
     *     <li>Parses the {@link PendingTasksAwait#pointcut()} selector and registers the
     *     corresponding type and method matchers.</li>
     *     <li>Registers advice on matching constructors or factory methods so that created
     *     service instances are tracked by {@link PendingTasksTracker}.</li>
     * </ul>
     *
     * @param awaits       collection of await configurations that describe what to track and how to count pending tasks
     * @param agentBuilder base {@link AgentBuilder} instance to extend with pending-tasks instrumentation
     * @return the supplied {@link AgentBuilder} extended with all configured transformations
     * @throws ReflectiveOperationException if a custom count function class cannot be instantiated
     * @throws IllegalArgumentException     if neither {@link PendingTasksAwait#countPendingTasksMethod()}
     *                                      nor {@link PendingTasksAwait#countPendingTasksFunction()} is provided
     */
    public static AgentBuilder setUp(Collection<PendingTasksAwait> awaits, AgentBuilder agentBuilder) throws ReflectiveOperationException {
        PendingTasksTracker.setClassFilter(new ClassPredicate());
        for (PendingTasksAwait await : awaits) {
            Class<? extends SneakyFunction<Object, Integer, ? extends Exception>> functionClass =
                    await.countPendingTasksFunction();
            SneakyFunction<Object, Integer, ? extends Exception> function;
            if (functionClass == PendingTasksAwait.NoFunction.class) {
                if (await.countPendingTasksMethod().isEmpty()) {
                    throw new IllegalArgumentException(
                            "Provide PendingTasksAwait.countPendingTasksMethod() or PendingTasksAwait.countPendingTasksFunction()");
                }
                function = new ReflectionFunction(await.countPendingTasksMethod());
            } else {
                function = functionClass.getDeclaredConstructor().newInstance();
            }
            ByteBuddySelectorResult result = ByteBuddySelectorParser.parse(await.pointcut());
            PendingTasksTracker.addCountFunction(await.pointcut(), SneakyFunction.sneaky(function));
            ClassPredicate.put(await.pointcut(), result.getTypeMatcher());
            if (result.getMethodMatcher() == null) {
                agentBuilder = configureTypeTransformation(PendingTasksConstructorAdvice.class, agentBuilder,
                        result.getTypeMatcher(), ElementMatchers.isConstructor(), await.pointcut());
            } else {
                agentBuilder = configureTypeTransformation(PendingTasksFactoryMethodAdvice.class, agentBuilder,
                        result.getTypeMatcher(), result.getMethodMatcher().and(ElementMatchers.isMethod())
                                .and(ElementMatchers.not(ElementMatchers.isConstructor())), await.pointcut());
            }
        }
        return agentBuilder;
    }

    /**
     * Clears all registered pointcut matchers and tracked services.
     * Intended to be called between tests (or at agent shutdown) to reset global state.
     */
    public static void tearDown() {
        ClassPredicate.clear();
        PendingTasksTracker.clear();
    }

    /**
     * No-op; tracking is done via Byte Buddy advice when matching types are instantiated.
     */
    @Override
    public void start() {
    }

    /**
     * {@inheritDoc}
     * Returns {@code true} if at least one tracked service currently has pending tasks.
     */
    @Override
    public boolean isAnyTaskStarted() {
        return PendingTasksTracker.getCount(false) > 0;
    }

    /**
     * {@inheritDoc}
     * Blocks by polling the total pending-task count across all tracked services until it reaches zero.
     */
    @Override
    @SneakyThrows
    public void waitCompletion() {
        while (PendingTasksTracker.getCount(true) > 0) {
            Thread.sleep(1);
        }
        log.debug("No services with tasks");
    }

    /**
     * Applies Byte Buddy advice to constructors or methods matching the given matchers,
     * so that created instances are registered with {@link PendingTasksTracker} for the pointcut.
     *
     * @param advice        constructor or factory-method advice class to apply
     * @param agentBuilder  agent builder to extend
     * @param typeMatcher   matcher for target types
     * @param methodMatcher matcher for target constructors or methods
     * @param pointcut      pointcut string passed to advice via {@link Pointcut}
     * @return the extended {@link AgentBuilder}
     */
    private static AgentBuilder configureTypeTransformation(
            Class<?> advice, AgentBuilder agentBuilder, ElementMatcher<? super TypeDescription> typeMatcher,
            ElementMatcher<? super MethodDescription> methodMatcher, String pointcut
    ) {
        return agentBuilder
                .type(typeMatcher)
                .transform((builder, type, classLoader, module, domain) ->
                        builder.visit(Advice.withCustomMapping().bind(Pointcut.class, pointcut)
                                        .to(advice).on(methodMatcher))
                                .visit(ByteBuddyUtils.getParameterWritingVisitorWrapper().apply(type))
                );
    }
}