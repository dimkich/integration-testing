package io.github.dimkich.integration.testing.util;

import java.io.File;
import java.net.URL;

public class TestUtils {
    public static File getTestResourceFile(String path) {
        URL resource = TestUtils.class.getClassLoader().getResource(".");
        if (resource == null) {
            throw new IllegalStateException("Test resource directory is not found");
        }
        return new File(resource.getFile() + "../../src/test/resources/" + path);
    }
}
