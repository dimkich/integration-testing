package io.github.dimkich.integration.testing;

@FunctionalInterface
public interface BeforeTestCase {
    void before(TestCase testCase) throws Exception;
}
