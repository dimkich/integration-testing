package io.github.dimkich.integration.testing.initialization;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dimkich.integration.testing.TestCaseInit;
import io.github.dimkich.integration.testing.storage.TestDataStorages;
import io.github.dimkich.integration.testing.storage.keyvalue.KeyValueDataStorage;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = false)
public class KeyValueStorageInit extends TestCaseInit {
    @JacksonXmlProperty(isAttribute = true)
    private String name;
    @JacksonXmlProperty(isAttribute = true)
    private Boolean clear;
    private Map<String, Object> map;

    @Override
    public Integer getOrder() {
        return 3000;
    }

    @Component
    @RequiredArgsConstructor
    public static class Initializer implements TestCaseInitializer<KeyValueStorageInit> {
        private final TestDataStorages testDataStorages;

        @Override
        public Class<KeyValueStorageInit> getTestCaseInitClass() {
            return KeyValueStorageInit.class;
        }

        @Override
        public void init(KeyValueStorageInit init) throws Exception {
            KeyValueDataStorage storage = testDataStorages.getTestDataStorage(init.getName(),
                    KeyValueDataStorage.class);
            if (init.getClear() != null && init.getClear()) {
                storage.clearAll();
            }
            if (init.getMap() != null) {
                storage.putKeysData(init.getMap());
            }
        }
    }
}
