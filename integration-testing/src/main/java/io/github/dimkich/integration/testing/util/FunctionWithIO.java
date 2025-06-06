package io.github.dimkich.integration.testing.util;

import java.io.IOException;

@FunctionalInterface
public interface FunctionWithIO<T, R> {
    R apply(T t) throws IOException;
}
