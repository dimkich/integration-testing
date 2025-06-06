package io.github.dimkich.integration.testing;

@FunctionalInterface
public interface BeforeTestCase {
    void accept(TestCase testCase) throws Exception;
}
