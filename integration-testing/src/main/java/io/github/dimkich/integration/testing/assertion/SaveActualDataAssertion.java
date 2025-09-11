package io.github.dimkich.integration.testing.assertion;

import io.github.dimkich.integration.testing.Assertion;
import io.github.dimkich.integration.testing.Test;
import io.github.dimkich.integration.testing.format.CompositeTestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@RequiredArgsConstructor
@ConditionalOnProperty(value = AssertionConfig.ASSERTION_PROPERTY, havingValue = "SaveActualData")
public class SaveActualDataAssertion implements Assertion {
    private final CompositeTestMapper mapper;

    @Override
    public void setExpected(Test expected) {
    }

    @Override
    public void assertTestsEquals(Test actual) {
    }

    @Override
    public void afterTests(Test rootTest) throws Exception {
        mapper.writeRootTest(rootTest);
    }
}
