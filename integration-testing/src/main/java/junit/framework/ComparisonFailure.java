package junit.framework;

import lombok.Getter;

/**
 * Required to Intellij Idea for showing file comparison dialog, instead of string comparison, on assertion failure
 */
@Getter
public class ComparisonFailure extends AssertionError {
    private final String actual;
    private final String expected;

    public ComparisonFailure(String message, String expected, String actual) {
        super(message);
        this.expected = expected;
        this.actual = actual;
    }

    @Override
    public String getMessage() {
        return "error message expected:<[]> but was:<[]>";
    }
}
