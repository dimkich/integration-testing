package io.github.dimkich.integration.testing.execution;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the result of a mock invocation, which can be either a return value or a thrown exception.
 * <p>
 * This class is used within {@link MockInvoke} to define the sequence of results/exceptions that should
 * be produced on subsequent invocations of a mocked method. Only one of the two fields should be set:
 * either a return value or an exception to throw.
 * <p>
 * The class uses Jackson annotations to handle JSON serialization, mapping the fields to "return" and "throw"
 * property names (which are Java keywords, hence the field names use numeric suffixes).
 *
 * @see MockInvoke
 */
@Data
@NoArgsConstructor
public class MockInvokeResult {
    /**
     * The return value for this mock invocation result.
     * <p>
     * This field should be set when the mock should return a value normally. It will be serialized
     * to JSON with the property name "return". Mutually exclusive with {@link #throw1}.
     */
    @JsonProperty("return")
    private Object return1;

    /**
     * The exception to throw for this mock invocation result.
     * <p>
     * This field should be set when the mock should throw an exception. It will be serialized
     * to JSON with the property name "throw". Mutually exclusive with {@link #return1}.
     */
    @JsonProperty("throw")
    private Throwable throw1;

    /**
     * Creates a result that represents a normal return value.
     *
     * @param return1 the value to return from the mock invocation
     */
    public MockInvokeResult(Object return1) {
        this.return1 = return1;
    }

    /**
     * Creates a result that represents an exception to throw.
     *
     * @param throw1 the exception to throw from the mock invocation
     */
    public MockInvokeResult(Throwable throw1) {
        this.throw1 = throw1;
    }
}
