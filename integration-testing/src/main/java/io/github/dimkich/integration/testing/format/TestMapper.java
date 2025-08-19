package io.github.dimkich.integration.testing.format;

import io.github.dimkich.integration.testing.Test;

import java.io.IOException;

public interface TestMapper {
    void setPath(String path);

    String getFilePath();

    Test readAllTests() throws IOException;

    void writeRootTest(Test test) throws IOException;

    String getRootTestAsString(Test test) throws IOException;

    String getSingleTestAsString(Test test) throws IOException;

    String getCurrentPathAndLocation(Test test) throws IOException;

    Object unwrap();
}
