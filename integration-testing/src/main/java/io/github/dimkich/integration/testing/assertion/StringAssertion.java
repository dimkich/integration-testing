package io.github.dimkich.integration.testing.assertion;

import io.github.dimkich.integration.testing.Assertion;
import io.github.dimkich.integration.testing.Test;
import io.github.dimkich.integration.testing.format.CompositeTestMapper;
import org.junit.jupiter.api.Assertions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@ConditionalOnProperty(value = AssertionConfig.ASSERTION_PROPERTY, havingValue = "string")
public class StringAssertion implements Assertion {

    @Override
    public void assertTestsEquals(CompositeTestMapper mapper, Test expected, Test actual) throws Exception {
        System.out.println(); // IntelliJ Idea can't normally parse diff without new line
        Assertions.assertEquals(mapper.getSingleTestAsString(expected), mapper.getSingleTestAsString(actual));
    }

    @Override
    public void afterTests(CompositeTestMapper mapper, Test rootTest) {
    }
}
