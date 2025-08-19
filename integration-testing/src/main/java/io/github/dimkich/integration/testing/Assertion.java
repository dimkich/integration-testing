package io.github.dimkich.integration.testing;

import io.github.dimkich.integration.testing.format.CompositeTestMapper;

public interface Assertion {
    default boolean makeTestDeepClone() {
        return true;
    }

    void assertTestsEquals(CompositeTestMapper mapper, Test expected, Test actual) throws Exception;

    void afterTests(CompositeTestMapper mapper, Test rootTest) throws Exception;
}
