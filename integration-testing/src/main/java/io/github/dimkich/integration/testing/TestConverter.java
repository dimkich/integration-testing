package io.github.dimkich.integration.testing;

import lombok.SneakyThrows;

/**
 * Functional interface for converting or transforming test data during test execution.
 * <p>
 * Test converters are applied to test instances after test execution completes but before
 * assertions are performed. They allow for dynamic transformation of test data, such as:
 * <ul>
 *   <li>Converting response values to different formats</li>
 *   <li>Transforming outbound messages</li>
 *   <li>Modifying data storage differences</li>
 *   <li>Adjusting custom test data</li>
 * </ul>
 * <p>
 * Converters are executed in the order they are provided to the {@link io.github.dimkich.integration.testing.execution.TestExecutor}.
 * Each converter receives the same test instance and can modify it in place.
 * <p>
 * This interface is a functional interface, so it can be used with lambda expressions or method references.
 *
 * @author dimkich
 * @see Test
 * @see io.github.dimkich.integration.testing.execution.TestExecutor
 */
@FunctionalInterface
public interface TestConverter {
    /**
     * Converts or transforms the given test instance.
     * <p>
     * This method is called after test execution completes and can modify the test
     * instance in place. Common use cases include converting response values,
     * transforming messages, or adjusting test data for assertion purposes.
     *
     * @param test the test instance to convert or transform
     * @throws Exception if an error occurs during conversion
     */
    void convert(Test test) throws Exception;

    /**
     * Converts or transforms the given test instance without throwing checked exceptions.
     * <p>
     * This is a convenience method that wraps {@link #convert(Test)} and uses
     * {@link lombok.SneakyThrows} to avoid checked exception handling. It is useful
     * when converters are used in contexts where checked exceptions are not desired,
     * such as in lambda expressions or forEach loops.
     *
     * @param test the test instance to convert or transform
     */
    @SneakyThrows
    default void convertNoException(Test test) {
        convert(test);
    }
}
