package io.github.dimkich.integration.testing.initialization;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dimkich.integration.testing.Test;
import io.github.dimkich.integration.testing.TestInit;
import io.github.dimkich.integration.testing.execution.MockInvoke;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.stream.Stream;

@Getter
@Setter
@ToString
public class MockInit extends TestInit {
    @JacksonXmlProperty(isAttribute = true)
    private Boolean resetAll;

    public static class Init implements Initializer<MockInit> {
        @Override
        public Class<MockInit> getTestInitClass() {
            return MockInit.class;
        }

        @Override
        public Integer getOrder() {
            return 4000;
        }

        @Override
        public void init(Stream<MockInit> inits) {
            inits.forEach(init -> {
                if (init.getResetAll() != null && init.getResetAll()) {
                    Test test = init.getTest();
                    while (test != null) {
                        test.getMockInvoke().forEach(MockInvoke::reset);
                        test = test.getParentTest();
                    }
                }
            });
        }
    }
}
