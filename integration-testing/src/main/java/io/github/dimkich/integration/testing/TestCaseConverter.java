package io.github.dimkich.integration.testing;

import lombok.SneakyThrows;

@FunctionalInterface
public interface TestCaseConverter {
    void convert(TestCase testCase) throws Exception;

    @SneakyThrows
    default void convertNoException(TestCase testCase) {
        convert(testCase);
    }
}
