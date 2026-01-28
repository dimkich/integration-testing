package io.github.dimkich.integration.testing.wait.completion.future.like;

import eu.ciechanowiec.sneakyfun.SneakyConsumer;
import io.github.dimkich.integration.testing.util.ByteBuddyUtils;
import io.github.dimkich.integration.testing.wait.completion.ClassPredicate;
import io.github.dimkich.integration.testing.wait.completion.FutureLikeAwait;
import io.github.dimkich.integration.testing.wait.completion.Pointcut;
import io.github.dimkich.integration.testing.wait.completion.WaitCompletion;
import io.github.dimkich.integration.testing.wait.completion.parser.ByteBuddySelectorParser;
import io.github.dimkich.integration.testing.wait.completion.parser.ByteBuddySelectorResult;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import java.util.Collection;

@Slf4j
@RequiredArgsConstructor
/**
 * {@link WaitCompletion} implementation that tracks and waits for completion of
 * "future-like" objects created via constructors or factory methods matched by
 * {@link FutureLikeAwait} pointcuts.
 * <p>
 * This class configures Byte Buddy instrumentation at agent startup, delegates
 * tracking of created instances to {@link FutureLikeTracker} and provides a
 * simple lifecycle API used from tests to reset tracking, detect active tasks
 * and block until all of them are completed.
 */
public class FutureLikeWaitCompletion implements WaitCompletion {

    /**
     * Configures Byte Buddy instrumentation for all {@link FutureLikeAwait} definitions.
     * <p>
     * For every await definition this method:
     * <ul>
     *     <li>Resolves the await consumer either via {@link FutureLikeAwait#awaitConsumer()} or
     *     {@link FutureLikeAwait#awaitMethod()} (reflection based fallback).</li>
     *     <li>Parses the {@link FutureLikeAwait#pointcut()} selector and registers the corresponding
     *     type and method matchers.</li>
     *     <li>Registers advice on matching constructors or factory methods so that created
     *     "future-like" instances are tracked by {@link FutureLikeTracker}.</li>
     * </ul>
     *
     * @param awaits       collection of await configurations that describe what and how to wait for
     * @param agentBuilder base {@link AgentBuilder} instance to extend with future-like instrumentation
     * @return the supplied {@link AgentBuilder} extended with all configured transformations
     * @throws ReflectiveOperationException if a custom consumer class cannot be instantiated
     * @throws IllegalArgumentException     if neither {@link FutureLikeAwait#awaitMethod()} nor
     *                                      {@link FutureLikeAwait#awaitConsumer()} is provided
     */
    public static AgentBuilder setUp(Collection<FutureLikeAwait> awaits, AgentBuilder agentBuilder) throws ReflectiveOperationException {
        FutureLikeTracker.setClassFilter(new ClassPredicate());
        for (FutureLikeAwait await : awaits) {
            Class<? extends SneakyConsumer<Object, ? extends Exception>> consumerClass = await.awaitConsumer();
            SneakyConsumer<Object, ? extends Exception> consumer;
            if (consumerClass == FutureLikeAwait.NoConsumer.class) {
                if (await.awaitMethod().isEmpty()) {
                    throw new IllegalArgumentException(
                            "Provide FutureLikeAwait.awaitMethod() or FutureLikeAwait.awaitConsumer()");
                }
                consumer = new ReflectionConsumer(await.awaitMethod());
            } else {
                consumer = consumerClass.getDeclaredConstructor().newInstance();
            }
            ByteBuddySelectorResult result = ByteBuddySelectorParser.parse(await.pointcut());
            FutureLikeTracker.addAwaitConsumer(await.pointcut(), SneakyConsumer.sneaky(consumer));
            ClassPredicate.put(await.pointcut(), result.getTypeMatcher());
            if (result.getMethodMatcher() == null) {
                agentBuilder = configureTypeTransformation(FutureLikeConstructorAdvice.class, agentBuilder,
                        result.getTypeMatcher(), ElementMatchers.isConstructor(), await.pointcut());
            } else {
                agentBuilder = configureTypeTransformation(FutureLikeFactoryMethodAdvice.class, agentBuilder,
                        result.getTypeMatcher(), result.getMethodMatcher().and(ElementMatchers.isMethod())
                                .and(ElementMatchers.not(ElementMatchers.isConstructor())), await.pointcut());
            }
        }
        return agentBuilder;
    }

    /**
     * Clears all state associated with future-like tracking.
     * <p>
     * This removes registered class predicates and await consumers so that subsequent tests
     * or agent runs start from a clean state.
     */
    public static void tearDown() {
        ClassPredicate.clear();
        FutureLikeTracker.clearAwaitConsumers();
    }

    /**
     * Starts tracking future-like tasks for the current test execution.
     * <p>
     * Internally this resets {@link FutureLikeTracker} so that only tasks created after
     * this call are taken into account.
     */
    @Override
    public void start() {
        FutureLikeTracker.reset();
    }

    /**
     * Indicates whether any tracked future-like task has been created but not yet completed.
     *
     * @return {@code true} if there is at least one active task, {@code false} otherwise
     */
    @Override
    public boolean isAnyTaskStarted() {
        return FutureLikeTracker.isAnyActivity();
    }

    /**
     * Blocks the calling thread until all tracked future-like tasks are completed.
     * <p>
     * The exact waiting strategy is delegated to {@link FutureLikeTracker}.
     */
    @Override
    @SneakyThrows
    public void waitCompletion() {
        FutureLikeTracker.waitCompletion();
    }

    /**
     * Applies a single Byte Buddy transformation for matching types and methods.
     * <p>
     * The transformation:
     * <ul>
     *     <li>Applies the given {@code advice} to all methods matching {@code methodMatcher}.</li>
     *     <li>Binds the {@link Pointcut} annotation value so that advice can access the original selector string.</li>
     *     <li>Installs the parameter writing visitor wrapper from {@link ByteBuddyUtils} to allow
     *     capturing method arguments.</li>
     * </ul>
     *
     * @param advice        advice class to apply
     * @param agentBuilder  builder to attach the transformation to
     * @param typeMatcher   matcher for instrumented types
     * @param methodMatcher matcher for instrumented methods within matching types
     * @param pointcut      original selector string used for logging and tracking
     * @return updated {@link AgentBuilder} with the transformation applied
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
