package io.github.dimkich.integration.testing.assertion;

import io.github.dimkich.integration.testing.Assertion;
import io.github.dimkich.integration.testing.TestCase;
import io.github.dimkich.integration.testing.TestCaseMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@ConditionalOnProperty(value = AssertionConfig.ASSERTION_PROPERTY, havingValue = "saveActualData")
public class SaveActualDataAssertion implements Assertion {
    @Override
    public boolean makeTestCaseDeepClone() {
        return false;
    }

    @Override
    public void assertTestCaseEquals(TestCaseMapper mapper, TestCase expected, TestCase actual) {
    }

    @Override
    public void afterTests(TestCaseMapper mapper, TestCase rootTestCase) throws Exception {
        mapper.writeRootTestCase(rootTestCase);
    }
}
