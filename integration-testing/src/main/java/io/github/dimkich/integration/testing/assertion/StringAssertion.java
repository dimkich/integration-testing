package io.github.dimkich.integration.testing.assertion;

import io.github.dimkich.integration.testing.Assertion;
import io.github.dimkich.integration.testing.Test;
import io.github.dimkich.integration.testing.format.CompositeTestMapper;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.io.IOException;

@RequiredArgsConstructor
@ConditionalOnProperty(value = AssertionConfig.ASSERTION_PROPERTY, havingValue = "String")
public class StringAssertion implements Assertion {
    private final CompositeTestMapper mapper;
    private String expected;

    @Override
    public void setExpected(Test expected) throws Exception {
        this.expected = mapper.getSingleTestAsString(expected);
    }

    @Override
    public void assertTestsEquals(Test actual) throws IOException {
        System.out.println(); // IntelliJ Idea can't normally parse diff without new line
        Assertions.assertEquals(expected, mapper.getSingleTestAsString(actual));
    }

    @Override
    public void afterTests(Test rootTest) {
    }
}
