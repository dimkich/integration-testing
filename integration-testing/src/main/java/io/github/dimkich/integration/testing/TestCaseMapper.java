package io.github.dimkich.integration.testing;

import java.io.IOException;

public interface TestCaseMapper {
    void setPath(String path);

    String getFilePath();

    TestCase readAllTestCases() throws IOException;

    void writeRootTestCase(TestCase testCase) throws IOException;

    String getRootTestCaseAsString(TestCase testCase) throws IOException;

    String getSingleTestCaseAsString(TestCase testCase) throws IOException;

    <T> T deepClone(T object);

    String getCurrentPathAndLocation(TestCase testCase) throws IOException;

    Object unwrap();
}
