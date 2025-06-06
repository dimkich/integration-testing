package io.github.dimkich.integration.testing.util;

import java.io.File;

public class TestUtils {
    public static File getTestResourceFile(String path) {
        return new File(
                TestUtils.class
                        .getClassLoader()
                        .getResource(".")
                        .getFile() + "../../src/test/resources/" + path
        );
    }
}
