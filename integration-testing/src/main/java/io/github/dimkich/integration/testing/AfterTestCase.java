package io.github.dimkich.integration.testing;

public interface AfterTestCase {
    void accept(TestCase testCase) throws Exception;
}
