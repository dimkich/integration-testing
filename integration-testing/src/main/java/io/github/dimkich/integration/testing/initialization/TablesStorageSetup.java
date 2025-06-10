package io.github.dimkich.integration.testing.initialization;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dimkich.integration.testing.TestCaseInit;
import io.github.dimkich.integration.testing.storage.TestDataStorages;
import io.github.dimkich.integration.testing.storage.sql.SQLDataStorageService;
import lombok.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@ToString
public class TablesStorageSetup extends TestCaseInit {
    @JacksonXmlProperty(isAttribute = true)
    private String name;
    private List<String> dbUnitPath;
    private List<TableCacheReload> cacheReload;

    @Data
    public static class TableCacheReload {
        @JacksonXmlProperty(isAttribute = true)
        private String tableName;
        @JacksonXmlProperty(isAttribute = true)
        private String beanName;
        @JacksonXmlProperty(isAttribute = true)
        private String beanMethod;
    }

    @Component
    @RequiredArgsConstructor
    public static class Initializer implements TestCaseInitializer<TablesStorageSetup> {
        private final TestDataStorages testDataStorages;

        @Override
        public Class<TablesStorageSetup> getTestCaseInitClass() {
            return TablesStorageSetup.class;
        }

        @Override
        @SneakyThrows
        public void init(TablesStorageSetup init) {
            SQLDataStorageService storage = testDataStorages.getTestDataStorage(init.getName(), SQLDataStorageService.class);

            if (init.getDbUnitPath() != null) {
                storage.setDbUnitXml(init.getDbUnitPath());
            }
            if (init.getCacheReload() != null) {
                storage.setTableCacheReload(init.getCacheReload());
            }
        }
    }
}
