package io.github.dimkich.integration.testing.wait.completion;

import io.github.dimkich.integration.testing.IntegrationTesting;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Declares how to discover and wait for completion of activities represented by
 * method invocations.
 * <p>
 * The annotation is processed by {@link io.github.dimkich.integration.testing.wait.completion.method.counting.MethodCountingWaitCompletion}
 * at agent startup. For every declared {@link MethodCountingAwait} the framework:
 * <ul>
 *     <li>Parses {@link #pointcut()} using the Byte Buddy selector parser to
 *     find methods that should be tracked.</li>
 *     <li>Instruments the matched methods so that on every entry the internal
 *     counter in {@link io.github.dimkich.integration.testing.wait.completion.method.counting.MethodCountingTracker}
 *     is increased and on every (normal or exceptional) exit it is decreased.</li>
 *     <li>Uses this counter later, when tests call the wait-completion API, to
 *     repeatedly wait while the counter is greater than zero so that execution
 *     continues only after all tracked method activities have completed (or a
 *     timeout elapses).</li>
 * </ul>
 * Typical usage is to place this annotation on an integration test class and
 * describe which methods should be considered "activity" for the purpose of
 * wait-completion (via {@link #pointcut()}).
 */
@Inherited
@Documented
@IntegrationTesting
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(MethodCountingAwait.List.class)
public @interface MethodCountingAwait {

    /**
     * Selector that defines which methods represent tracked activity.
     * <p>
     * The selector is parsed by the wait-completion Byte Buddy selector parser
     * ({@link io.github.dimkich.integration.testing.wait.completion.parser.ByteBuddySelectorParser})
     * and translated into a type and method matcher. Unlike future-like tracking,
     * {@link io.github.dimkich.integration.testing.wait.completion.method.counting.MethodCountingWaitCompletion}
     * requires the selector to define a method matcher; otherwise an
     * {@link IllegalArgumentException} is thrown.
     *
     * @return selector describing the methods whose invocations should be counted
     */
    String pointcut();

    /**
     * Container annotation for repeatable {@link MethodCountingAwait} declarations.
     */
    @Inherited
    @Documented
    @Target(TYPE)
    @Retention(RUNTIME)
    @interface List {
        MethodCountingAwait[] value();
    }
}
