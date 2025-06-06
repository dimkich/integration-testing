package io.github.dimkich.integration.testing.assertion;

import io.github.dimkich.integration.testing.Assertion;
import io.github.dimkich.integration.testing.TestCase;
import io.github.dimkich.integration.testing.TestCaseMapper;
import org.junit.jupiter.api.Assertions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@ConditionalOnProperty(value = AssertionConfig.ASSERTION_PROPERTY, havingValue = "string")
public class StringAssertion implements Assertion {

    @Override
    public void assertTestCaseEquals(TestCaseMapper mapper, TestCase expected, TestCase actual) throws Exception {
        System.out.println(); // IntelliJ Idea can't normally parse diff without new line
        Assertions.assertEquals(mapper.getSingleTestCaseAsString(expected), mapper.getSingleTestCaseAsString(actual));
    }

    @Override
    public void afterTests(TestCaseMapper mapper, TestCase rootTestCase) {
    }
}
