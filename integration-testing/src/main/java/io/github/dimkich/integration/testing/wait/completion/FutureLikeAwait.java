package io.github.dimkich.integration.testing.wait.completion;

import eu.ciechanowiec.sneakyfun.SneakyConsumer;
import io.github.dimkich.integration.testing.IntegrationTesting;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Declares how to discover and wait for completion of "future-like" objects.
 * <p>
 * The annotation is processed by {@code FutureLikeWaitCompletion} at agent startup.
 * For every declared {@link FutureLikeAwait} the framework:
 * <ul>
 *     <li>Parses {@link #pointcut()} using the Byte Buddy selector parser to find
 *     constructors and/or factory methods that create "future-like" instances.</li>
 *     <li>Instruments the matched constructors or factory methods so that every
 *     created instance is registered in the {@code FutureLikeTracker} together
 *     with a per-pointcut await strategy.</li>
 *     <li>Uses that await strategy later, when tests call the wait-completion
 *     API, to actually block until all tracked instances are completed.</li>
 * </ul>
 * Typical usage is to put this annotation on an integration test class and
 * describe "where future-like objects come from" (via {@link #pointcut()})
 * and "how to wait for them" (via {@link #awaitMethod()} or {@link #awaitConsumer()}).
 */
@Inherited
@Documented
@IntegrationTesting
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(FutureLikeAwait.List.class)
public @interface FutureLikeAwait {
    /**
     * Selector that defines which classes and factory methods create "future-like" objects.
     * <p>
     * The selector is parsed by the wait-completion Byte Buddy selector parser and
     * translated into a {@code TypeDescription} and (optionally) a {@code MethodDescription}
     * matcher. If the method matcher is missing, all constructors of the matched
     * type are instrumented; otherwise only matching factory methods are instrumented.
     * In both cases, every created instance is handed over to the configured await
     * strategy so that it can be tracked and later awaited.
     * <p>
     * For the selector syntax see {@link io.github.dimkich.integration.testing.wait.completion.parser.ByteBuddySelectorParser}.
     */
    String pointcut();

    /**
     * Name of an instance method that blocks until the represented computation is complete.
     * <p>
     * If specified, this no-arg method will be invoked reflectively on each
     * tracked instance when the framework needs to wait for completion.
     * <p>
     * This is a convenience for existing "future-like" types that already expose
     * a suitable blocking method such as {@code join()}, {@code get()} or similar.
     * Either {@code awaitMethod()} or {@link #awaitConsumer()} must be provided.
     */
    String awaitMethod() default "";

    /**
     * Consumer strategy that performs the await logic on the matched object.
     * <p>
     * The consumer receives each "future-like" instance and is expected to block
     * until the corresponding asynchronous computation has completed. This gives
     * full control over how "completion" is detected (for example, by polling,
     * delegating to a third‑party API or composing several futures).
     * <p>
     * If a concrete consumer class is specified here, it is instantiated once
     * per {@link FutureLikeAwait} configuration and then used whenever the
     * wait-completion mechanism needs to wait for all tracked tasks.
     * Either {@link #awaitConsumer()} or {@code awaitMethod()} must be provided.
     */
    Class<? extends SneakyConsumer<Object, ? extends Exception>> awaitConsumer() default NoConsumer.class;

    /**
     * Container annotation for repeatable {@link FutureLikeAwait} declarations.
     */
    @Inherited
    @Documented
    @Target(TYPE)
    @Retention(RUNTIME)
    @interface List {
        FutureLikeAwait[] value();
    }

    /**
     * Default consumer used when no await strategy is configured.
     * <p>
     * Always throws {@link UnsupportedOperationException} to signal a misconfiguration.
     */
    final class NoConsumer implements SneakyConsumer<Object, Exception> {
        @Override
        public void accept(Object input) {
            throw new UnsupportedOperationException(
                    "No await strategy provided. Specify either awaitMethod() or awaitConsumer()."
            );
        }
    }
}
