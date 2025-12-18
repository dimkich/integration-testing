package junit.framework;

import lombok.Getter;

/**
 * Represents a failure when comparing expected and actual values in a test assertion.
 * This class is required by IntelliJ IDEA to display a file comparison dialog instead of
 * a simple string comparison when an assertion fails.
 * <p>
 * By extending {@link AssertionError} and providing structured access to expected and actual values,
 * IDEs can provide enhanced error visualization capabilities.
 */
@Getter
public class ComparisonFailure extends AssertionError {
    /**
     * The actual value that was received during the test.
     */
    private final String actual;

    /**
     * The expected value that was anticipated during the test.
     */
    private final String expected;

    /**
     * Constructs a new {@code ComparisonFailure} with the specified message and comparison values.
     *
     * @param message  the detail message describing the failure
     * @param expected the expected value
     * @param actual   the actual value that was received
     */
    public ComparisonFailure(String message, String expected, String actual) {
        super(message);
        this.expected = expected;
        this.actual = actual;
    }

    /**
     * Returns a formatted error message indicating the comparison failure.
     * The message includes placeholders for expected and actual values.
     *
     * @return a formatted error message string
     */
    @Override
    public String getMessage() {
        return "error message expected:<[]> but was:<[]>";
    }
}
