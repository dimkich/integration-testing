package io.github.dimkich.integration.testing.util;

import java.io.File;
import java.lang.reflect.Array;

public class TestUtils {
    public static File getTestResourceFile(String path) {
        return new File(
                TestUtils.class
                        .getClassLoader()
                        .getResource(".")
                        .getFile() + "../../src/test/resources/" + path
        );
    }

    @SuppressWarnings("unchecked")
    public static <T> T getDefaultValue(Class<T> clazz) {
        return (T) Array.get(Array.newInstance(clazz, 1), 0);
    }
}
