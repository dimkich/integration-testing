package io.github.dimkich.integration.testing;

import lombok.SneakyThrows;

@FunctionalInterface
public interface TestConverter {
    void convert(Test test) throws Exception;

    @SneakyThrows
    default void convertNoException(Test test) {
        convert(test);
    }
}
