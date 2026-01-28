package io.github.dimkich.integration.testing.wait.completion;

import io.github.dimkich.integration.testing.IntegrationTesting;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Declares a pair of methods that define the start and end of an activity
 * which can be waited for during integration tests.
 * <p>
 * The annotation is processed by {@link io.github.dimkich.integration.testing.wait.completion.method.pair.MethodPairWaitCompletion}
 * at agent startup. For every declared {@link MethodPairAwait} the framework:
 * <ul>
 *     <li>Parses {@linkplain #startPointcut() start} and {@linkplain #endPointcut() end}
 *     selectors using the Byte Buddy selector parser.</li>
 *     <li>Installs Byte Buddy instrumentation so that calls to the matched "start"
 *     methods register an in-progress activity and calls to the matched "end"
 *     methods mark that activity as completed.</li>
 *     <li>Uses {@code MethodPairTracker} later, when tests call the wait-completion
 *     API, to block until all started activities have corresponding end calls.</li>
 * </ul>
 * Typical usage is to put this annotation on an integration test class and describe
 * which methods should be treated as the logical "enter" and "exit" points of an
 * asynchronous or long-running operation.
 */
@Inherited
@Documented
@IntegrationTesting
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(MethodPairAwait.List.class)
public @interface MethodPairAwait {
    /**
     * Selector that defines the methods which mark the start of a tracked activity.
     * <p>
     * The selector is parsed by the wait-completion Byte Buddy selector parser and
     * must define a method matcher; otherwise an {@link IllegalArgumentException}
     * is thrown during agent setup.
     * <p>
     * For the selector syntax see
     * {@link io.github.dimkich.integration.testing.wait.completion.parser.ByteBuddySelectorParser}.
     */
    String startPointcut();

    /**
     * Selector that defines the methods which mark the end of a tracked activity.
     * <p>
     * The selector is parsed by the wait-completion Byte Buddy selector parser and
     * must define a method matcher; otherwise an {@link IllegalArgumentException}
     * is thrown during agent setup.
     * <p>
     * For the selector syntax see
     * {@link io.github.dimkich.integration.testing.wait.completion.parser.ByteBuddySelectorParser}.
     */
    String endPointcut();

    /**
     * Container annotation for repeatable {@link MethodPairAwait} declarations.
     */
    @Inherited
    @Documented
    @Target(TYPE)
    @Retention(RUNTIME)
    @interface List {
        MethodPairAwait[] value();
    }
}
