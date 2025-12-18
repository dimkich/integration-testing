package io.github.dimkich.integration.testing;

import io.github.dimkich.integration.testing.execution.TestExecutor;

/**
 * Functional interface for executing after-test hooks in the integration testing framework.
 * <p>
 * Implementations of this interface are called after a test completes execution, allowing
 * for cleanup operations, resource deallocation, or post-processing of test results.
 * <p>
 * This interface is used as an extension point in {@link TestExecutor} to support custom
 * after-test behavior. Multiple {@code AfterTest} implementations can be registered and
 * will be executed in sequence after each test completes.
 * <p>
 * The after hook is called during the after phase of test execution, which occurs after
 * the test method execution, message handling, and mock verification have completed.
 *
 * @author dimkich
 * @see TestExecutor
 * @see BeforeTest
 * @see Test
 */
@FunctionalInterface
public interface AfterTest {
    /**
     * Executes the after-test hook for the given test.
     * <p>
     * This method is called after the test has completed execution, allowing for
     * cleanup, resource management, or any post-processing operations.
     *
     * @param test the test that has just completed execution
     * @throws Exception if an error occurs during the after-test hook execution
     */
    void after(Test test) throws Exception;
}
