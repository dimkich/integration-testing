package io.github.dimkich.integration.testing.initialization;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dimkich.integration.testing.TestCaseInit;
import io.github.dimkich.integration.testing.storage.DataSourceStorage;
import io.github.dimkich.integration.testing.storage.TestDataStorages;
import lombok.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@ToString
public class TablesStorageInit extends TestCaseInit {
    @JacksonXmlProperty(isAttribute = true)
    private String name;
    @JacksonXmlProperty(isAttribute = true)
    private Boolean loadAllTables;
    private String tablesToChange;
    private String tablesToLoad;
    private List<String> sqlFilePath;
    private List<String> sql;

    @Component
    @RequiredArgsConstructor
    public static class Initializer implements TestCaseInitializer<TablesStorageInit> {
        private final TestDataStorages testDataStorages;
        private final TablesStorageService tablesStorageService;

        @Override
        public Class<TablesStorageInit> getTestCaseInitClass() {
            return TablesStorageInit.class;
        }

        @Override
        @SneakyThrows
        public void init(TablesStorageInit init) {
            DataSourceStorage storage = testDataStorages.getTestDataStorage(init.getName(), DataSourceStorage.class);

            tablesStorageService.executeSqls(storage.getConnection(), init.getSqlFilePath(),
                    f -> new String(new ClassPathResource(f).getInputStream().readAllBytes(), StandardCharsets.UTF_8));
            tablesStorageService.executeSqls(storage.getConnection(), init.getSql(), s -> s);
            if (init.getTablesToChange() != null) {
                tablesStorageService.clearTables(storage, stringToList(init.getTablesToChange()));
            }
            if (init.getLoadAllTables() != null && init.getLoadAllTables()) {
                tablesStorageService.loadAllTablesData(storage);
            } else if (init.getTablesToLoad() != null) {
                tablesStorageService.loadTablesData(storage, stringToList(init.getTablesToLoad()));
            }
            testDataStorages.getMapDiff();
        }

        private Set<String> stringToList(String string) {
            return Arrays.stream(string.split(","))
                    .map(String::strip)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }
}