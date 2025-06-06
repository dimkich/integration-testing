package io.github.dimkich.integration.testing.initialization;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dimkich.integration.testing.TestCaseInit;
import io.github.dimkich.integration.testing.storage.TestDataStorages;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Data
@EqualsAndHashCode(callSuper = false)
public class TestDataStorageInit extends TestCaseInit {
    @JacksonXmlProperty(isAttribute = true)
    private Boolean clear;

    @Override
    public Integer getOrder() {
        return 3000;
    }

    @Component
    @RequiredArgsConstructor
    public static class Initializer implements TestCaseInitializer<TestDataStorageInit> {
        private final TestDataStorages testDataStorages;

        @Override
        public Class<TestDataStorageInit> getTestCaseInitClass() {
            return TestDataStorageInit.class;
        }

        @Override
        public void init(TestDataStorageInit init) {
            if (init.getClear() != null && init.getClear()) {
                testDataStorages.clear();
            }
        }
    }
}
