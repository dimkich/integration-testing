package io.github.dimkich.integration.testing;

/**
 * Functional interface for executing before-test hooks in the integration testing framework.
 * <p>
 * Implementations of this interface are called before a test begins execution, allowing
 * for setup operations, resource initialization, or pre-processing of test configuration.
 * <p>
 * This interface is used as an extension point in {@link TestExecutor} to support custom
 * before-test behavior. Multiple {@code BeforeTest} implementations can be registered and
 * will be executed in sequence before each test begins.
 * <p>
 * The before hook is called during the before phase of test execution, which occurs before
 * the test method execution, message handling, and mock verification begin.
 *
 * @author dimkich
 * @see TestExecutor
 * @see AfterTest
 * @see Test
 */
@FunctionalInterface
public interface BeforeTest {
    /**
     * Executes the before-test hook for the given test.
     * <p>
     * This method is called before the test begins execution, allowing for
     * setup, resource initialization, or any pre-processing operations.
     *
     * @param test the test that is about to begin execution
     * @throws Exception if an error occurs during the before-test hook execution
     */
    void before(Test test) throws Exception;
}
