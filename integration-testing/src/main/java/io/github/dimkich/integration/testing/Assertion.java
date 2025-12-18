package io.github.dimkich.integration.testing;

/**
 * Interface for comparing expected and actual test results in the integration testing framework.
 * <p>
 * Implementations of this interface provide different strategies for asserting test results:
 * <ul>
 *   <li><b>String-based comparison:</b> Compares serialized test representations as strings</li>
 *   <li><b>File-based comparison:</b> Writes expected and actual results to files for comparison</li>
 *   <li><b>Single file comparison:</b> Compares the entire test tree against a single file</li>
 *   <li><b>Save actual data:</b> Saves actual test results without comparison</li>
 * </ul>
 * <p>
 * The assertion lifecycle follows this sequence:
 * <ol>
 *   <li>{@link #setExpected(Test)} - Called before test execution to set the expected result</li>
 *   <li>{@link #assertTestsEquals(Test)} - Called after each test execution to compare results</li>
 *   <li>{@link #afterTests(Test)} - Called after all tests complete for final cleanup or assertions</li>
 * </ol>
 * <p>
 * Implementations are typically configured via Spring's {@code @ConditionalOnProperty} annotation
 * using the property {@code integration.testing.assertion}.
 *
 * @author dimkich
 * @see Test
 * @see io.github.dimkich.integration.testing.assertion.StringAssertion
 * @see io.github.dimkich.integration.testing.assertion.FileAssertion
 * @see io.github.dimkich.integration.testing.assertion.SingleFileAssertion
 * @see io.github.dimkich.integration.testing.assertion.SaveActualDataAssertion
 */
public interface Assertion {
    /**
     * Indicates whether this assertion implementation requires a temporary directory for test files.
     * <p>
     * When {@code true}, the test executor will create and manage a temporary directory
     * for storing test-related files (e.g., expected/actual comparison files).
     * <p>
     * The default implementation returns {@code false}, meaning no temporary directory is needed.
     *
     * @return {@code true} if a temporary directory should be created for test files,
     * {@code false} otherwise
     */
    default boolean useTestTempDir() {
        return false;
    }

    /**
     * Sets the expected test result before test execution.
     * <p>
     * This method is called once before the test execution begins to provide the expected
     * test result. Implementations should store this value for later comparison in
     * {@link #assertTestsEquals(Test)}.
     * <p>
     * Some implementations may choose to do nothing in this method if they perform
     * comparison at a different stage (e.g., in {@link #afterTests(Test)}).
     *
     * @param expected the expected test result
     * @throws Exception if an error occurs while setting the expected value
     */
    void setExpected(Test expected) throws Exception;

    /**
     * Compares the actual test result with the expected result.
     * <p>
     * This method is called after each test execution to verify that the actual result
     * matches the expected result. If the results do not match, implementations should
     * throw an appropriate exception (e.g., {@code AssertionError} or a custom exception
     * like {@code FileComparisonFailure}).
     * <p>
     * Some implementations may choose to do nothing in this method if they perform
     * comparison at a different stage (e.g., in {@link #afterTests(Test)}).
     *
     * @param actual the actual test result to compare
     * @throws Exception if the actual result does not match the expected result,
     *                   or if an error occurs during comparison
     */
    void assertTestsEquals(Test actual) throws Exception;

    /**
     * Performs final cleanup or assertions after all tests have completed.
     * <p>
     * This method is called once after all tests in the test tree have finished executing.
     * It receives the root test of the entire test hierarchy.
     * <p>
     * Implementations may use this method to:
     * <ul>
     *   <li>Perform final comparisons (e.g., comparing the entire test tree against a file)</li>
     *   <li>Save actual test results</li>
     *   <li>Clean up temporary resources</li>
     *   <li>Generate summary reports</li>
     * </ul>
     *
     * @param rootTest the root test of the entire test hierarchy
     * @throws Exception if an error occurs during cleanup or final assertions
     */
    void afterTests(Test rootTest) throws Exception;
}
