package io.github.dimkich.integration.testing.date.time;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.matcher.ElementMatchers;

import java.io.IOException;
import java.lang.instrument.Instrumentation;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * Configures libfaketime for Testcontainers via ByteBuddy instrumentation.
 * <p>
 * Installs a Java agent that intercepts {@link org.testcontainers.containers.GenericContainer#start()}
 * on all Testcontainers {@code GenericContainer} subclasses. When a container is started,
 * {@link LibFakeTimeAdvice} injects libfaketime configuration (LD_PRELOAD, shared library,
 * environment variables) for images matching the given include patterns.
 * </p>
 * <p>
 * This enables deterministic time control in integration tests: containers run with libfaketime
 * preloaded and connect to a shared faketime server, allowing {@link LibFakeTimeNowSetter}
 * to set the current time dynamically during test execution.
 * </p>
 *
 * @see LibFakeTimeAdvice
 * @see LibFakeTimeTracker
 * @see LibFakeTimeNowSetter
 */
public class LibFakeTimeSetUp {
    private static boolean initialized = false;

    /**
     * Initializes libfaketime support for Testcontainers.
     * <p>
     * Registers include patterns with {@link LibFakeTimeTracker} and installs the ByteBuddy
     * agent on the given instrumentation. The agent applies {@link LibFakeTimeAdvice} to
     * {@code GenericContainer.start()}, so only containers whose image name matches one of
     * the patterns will receive libfaketime configuration.
     * </p>
     * <p>
     * This method is idempotent: subsequent calls return immediately without re-installing
     * the agent, but patterns are updated on each invocation.
     * </p>
     *
     * @param instrumentation the {@link Instrumentation} instance (e.g. from a Java agent)
     * @param patterns        regex patterns for container image names to include, or {@code null}
     *                        or empty to exclude all containers from libfaketime
     * @throws Exception if agent installation or tracker setup fails
     */
    public static void setUp(Instrumentation instrumentation, String[] patterns) throws Exception {
        LibFakeTimeTracker.setUp(patterns);
        if (initialized) {
            return;
        }

        new AgentBuilder.Default()
                .disableClassFormatChanges()
                .with(new ByteBuddy().with(Implementation.Context.Disabled.Factory.INSTANCE))
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(AgentBuilder.RedefinitionStrategy.DiscoveryStrategy.Reiterating.INSTANCE)
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                .with(AgentBuilder.DescriptionStrategy.Default.POOL_FIRST)
                .type(ElementMatchers.hasSuperType(ElementMatchers.named("org.testcontainers.containers.GenericContainer")))
                .transform((builder, td, cl, module, domain) -> builder
                        .visit(Advice.to(LibFakeTimeAdvice.class)
                                .on(named("start")
                                        .and(takesArguments(0))
                                        .and(isPublic())))
                )
                .installOn(instrumentation);
        initialized = true;
    }

    /**
     * Releases libfaketime resources.
     * <p>
     * Clears the tracker state, closes the connection to the libfaketime daemon,
     * and stops the libfaketime-server container if one was started.
     * </p>
     *
     * @throws IOException if cleanup fails
     */
    public static void tearDown() throws IOException {
        LibFakeTimeTracker.tearDown();
    }
}