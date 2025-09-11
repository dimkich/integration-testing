package io.github.dimkich.integration.testing;

public interface Assertion {
    default boolean useTestTempDir() {
        return false;
    }

    void setExpected(Test expected) throws Exception;

    void assertTestsEquals(Test actual) throws Exception;

    void afterTests(Test rootTest) throws Exception;
}
