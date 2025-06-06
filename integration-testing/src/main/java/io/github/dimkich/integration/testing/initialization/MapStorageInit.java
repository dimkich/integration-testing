package io.github.dimkich.integration.testing.initialization;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dimkich.integration.testing.TestCaseInit;
import io.github.dimkich.integration.testing.storage.TestDataStorages;
import io.github.dimkich.integration.testing.storage.TestMapDataStorage;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = false)
public class MapStorageInit extends TestCaseInit {
    @JacksonXmlProperty(isAttribute = true)
    private String name;
    @JacksonXmlProperty(isAttribute = true)
    private Boolean clear;
    private Map<Object, Object> map;

    @Override
    public Integer getOrder() {
        return 2000;
    }

    @Component
    @RequiredArgsConstructor
    public static class Initializer implements TestCaseInitializer<MapStorageInit> {
        private final TestDataStorages testDataStorages;

        @Override
        public Class<MapStorageInit> getTestCaseInitClass() {
            return MapStorageInit.class;
        }

        @Override
        public void init(MapStorageInit init) {
            TestMapDataStorage storage = testDataStorages.getTestDataStorage(init.getName(), TestMapDataStorage.class);
            if (init.getClear()) {
                storage.clear();
            }
            if (init.getMap() != null) {
                storage.getMap().putAll(init.getMap());
            }
            testDataStorages.getMapDiff();
        }
    }
}
