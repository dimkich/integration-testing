package io.github.dimkich.integration.testing.execution.junit;

import io.github.dimkich.integration.testing.Test;
import io.github.dimkich.integration.testing.execution.TestExecutor;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.function.Executable;

/**
 * JUnit {@link Executable} implementation that runs an {@link Test}
 * using a configured {@link TestExecutor}.
 * <p>
 * The lifecycle is:
 * <ol>
 *     <li>invoke {@link TestExecutor#before(Test)} for the test,</li>
 *     <li>invoke {@link TestExecutor#runTest()},</li>
 *     <li>invoke {@link TestExecutor#after()} in a finally block, even when the before
 *         phase fails, so that partial initialization is always rolled back.</li>
 * </ol>
 */
@RequiredArgsConstructor
public class JunitExecutable implements Executable {
    private final Test test;
    private final TestExecutor testExecutor;

    /**
     * Executes the test using the associated {@link TestExecutor}.
     * <p>
     * The after phase is always invoked; if it also fails, its exception is attached to the
     * original failure as a suppressed exception.
     *
     * @throws Throwable if the underlying test execution throws any exception
     */
    @Override
    public void execute() throws Throwable {
        try {
            testExecutor.before(test);
            testExecutor.runTest();
        } catch (Throwable t) {
            try {
                testExecutor.after();
            } catch (Throwable afterFailure) {
                t.addSuppressed(afterFailure);
            }
            throw t;
        }
        testExecutor.after();
    }
}
