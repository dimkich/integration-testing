package io.github.dimkich.integration.testing.util;

public interface ConsumerWithException<T> {
    void accept(T t) throws Exception;
}
