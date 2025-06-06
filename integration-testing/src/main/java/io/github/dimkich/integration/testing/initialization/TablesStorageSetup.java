package io.github.dimkich.integration.testing.initialization;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dimkich.integration.testing.TestCaseInit;
import lombok.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@ToString
public class TablesStorageSetup extends TestCaseInit {
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
        private final TablesStorageService tablesStorageService;

        @Override
        public Class<TablesStorageSetup> getTestCaseInitClass() {
            return TablesStorageSetup.class;
        }

        @Override
        @SneakyThrows
        public void init(TablesStorageSetup init) {
            if (init.getDbUnitPath() != null) {
                tablesStorageService.addDbUnitXml(init.getDbUnitPath());
            }
            if (init.getCacheReload() != null) {
                init.getCacheReload().forEach(tablesStorageService::addCacheReload);
            }
        }
    }
}
