package io.github.dimkich.integration.testing;

public interface Assertion {
    default boolean makeTestCaseDeepClone() {
        return true;
    }

    void assertTestCaseEquals(TestCaseMapper mapper, TestCase expected, TestCase actual) throws Exception;

    void afterTests(TestCaseMapper mapper, TestCase rootTestCase) throws Exception;
}
