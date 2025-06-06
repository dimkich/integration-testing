package io.github.dimkich.integration.testing.util;

import java.io.IOException;

@FunctionalInterface
public interface SupplierWithIO<T> {
    T get() throws IOException;
}
