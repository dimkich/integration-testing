package io.github.dimkich.integration.testing.initialization;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dimkich.integration.testing.TestCase;
import io.github.dimkich.integration.testing.TestCaseInit;
import io.github.dimkich.integration.testing.execution.MockInvoke;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Collection;

@Getter
@Setter
@ToString
public class MockInit extends TestCaseInit {
    @JacksonXmlProperty(isAttribute = true)
    private Boolean resetAll;

    public static class Init implements Initializer<MockInit> {
        @Override
        public Class<MockInit> getTestCaseInitClass() {
            return MockInit.class;
        }

        @Override
        public Integer getOrder() {
            return 4000;
        }

        @Override
        public void init(Collection<MockInit> inits) {
            for (MockInit init : inits) {
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
}
