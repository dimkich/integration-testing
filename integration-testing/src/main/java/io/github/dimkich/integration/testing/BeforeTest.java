package io.github.dimkich.integration.testing;

@FunctionalInterface
public interface BeforeTest {
    void before(Test test) throws Exception;
}
