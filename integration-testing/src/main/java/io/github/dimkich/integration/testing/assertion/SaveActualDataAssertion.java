package io.github.dimkich.integration.testing.assertion;

import io.github.dimkich.integration.testing.Assertion;
import io.github.dimkich.integration.testing.Test;
import io.github.dimkich.integration.testing.TestMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@ConditionalOnProperty(value = AssertionConfig.ASSERTION_PROPERTY, havingValue = "saveActualData")
public class SaveActualDataAssertion implements Assertion {
    @Override
    public boolean makeTestDeepClone() {
        return false;
    }

    @Override
    public void assertTestsEquals(TestMapper mapper, Test expected, Test actual) {
    }

    @Override
    public void afterTests(TestMapper mapper, Test rootTest) throws Exception {
        mapper.writeRootTest(rootTest);
    }
}
