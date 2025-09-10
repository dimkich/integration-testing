package io.github.dimkich.integration.testing.assertion;

import io.github.dimkich.integration.testing.Assertion;
import io.github.dimkich.integration.testing.Test;
import io.github.dimkich.integration.testing.format.CompositeTestMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@ConditionalOnProperty(value = AssertionConfig.ASSERTION_PROPERTY, havingValue = "SaveActualData")
public class SaveActualDataAssertion implements Assertion {
    @Override
    public boolean makeTestDeepClone() {
        return false;
    }

    @Override
    public void assertTestsEquals(CompositeTestMapper mapper, Test expected, Test actual) {
    }

    @Override
    public void afterTests(CompositeTestMapper mapper, Test rootTest) throws Exception {
        mapper.writeRootTest(rootTest);
    }
}
