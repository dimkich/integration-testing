package io.github.dimkich.integration.testing.initialization;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dimkich.integration.testing.TestCase;
import io.github.dimkich.integration.testing.TestCaseInit;
import io.github.dimkich.integration.testing.execution.MockInvoke;
import lombok.*;

@Getter
@Setter
@ToString
public class MockInit extends TestCaseInit {
    @JacksonXmlProperty(isAttribute = true)
    private Boolean resetAll;

    @Override
    public Integer getOrder() {
        return 4000;
    }

    @RequiredArgsConstructor
    public static class Initializer implements TestCaseInitializer<MockInit> {

        @Override
        public Class<MockInit> getTestCaseInitClass() {
            return MockInit.class;
        }

        @Override
        @SneakyThrows
        public void init(MockInit init) {
            if (init.getResetAll() != null && init.getResetAll()) {
                TestCase testCase = init.getTestCase();
                while (testCase != null) {
                    testCase.getMockInvoke().forEach(MockInvoke::reset);
                    testCase = testCase.getParentTestCase();
                }
            }
        }
    }
}
