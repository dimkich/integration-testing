package io.github.dimkich.integration.testing.date.time;

import net.bytebuddy.asm.Advice;
import org.testcontainers.containers.GenericContainer;

/**
 * ByteBuddy advice that intercepts {@link GenericContainer#start()} to inject libfaketime
 * configuration before the container starts.
 * <p>
 * Applied via {@link LibFakeTimeSetUp} to all Testcontainers {@code GenericContainer} subclasses.
 * When a container is about to start, this advice delegates to {@link LibFakeTimeTracker#onEnter}
 * to configure LD_PRELOAD, the libfaketime shared library, and related environment variables,
 * enabling deterministic time control in integration tests.
 */
public class LibFakeTimeAdvice {

    /**
     * Invoked by ByteBuddy before the intercepted method (GenericContainer.start) executes.
     * Configures libfaketime on the container if the target object is a GenericContainer.
     *
     * @param object the receiver of the intercepted method (the container instance)
     */
    @Advice.OnMethodEnter
    @SuppressWarnings("unused")
    public static void enter(@Advice.This Object object) {
        if (object instanceof GenericContainer<?> container) {
            LibFakeTimeTracker.onEnter(container);
        }
    }
}
