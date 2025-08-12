package io.github.dimkich.integration.testing;

public interface Assertion {
    default boolean makeTestDeepClone() {
        return true;
    }

    void assertTestsEquals(TestMapper mapper, Test expected, Test actual) throws Exception;

    void afterTests(TestMapper mapper, Test rootTest) throws Exception;
}
