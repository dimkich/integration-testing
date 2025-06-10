package io.github.dimkich.integration.testing.initialization;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dimkich.integration.testing.TestCaseInit;
import io.github.dimkich.integration.testing.storage.TestDataStorages;
import io.github.dimkich.integration.testing.storage.sql.SQLDataStorageService;
import lombok.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
@ToString
public class TablesStorageInit extends TestCaseInit {
    @JacksonXmlProperty(isAttribute = true)
    private String name;
    @JacksonXmlProperty(isAttribute = true)
    private Boolean loadAllTables;
    @JacksonXmlProperty(isAttribute = true)
    private Boolean disableCacheReload;
    private List<String> sqlFilePath;
    private List<String> sql;
    private String tablesToChange;
    private String tablesToLoad;

    @Component
    @RequiredArgsConstructor
    public static class Initializer implements TestCaseInitializer<TablesStorageInit> {
        private final TestDataStorages testDataStorages;

        @Override
        public Class<TablesStorageInit> getTestCaseInitClass() {
            return TablesStorageInit.class;
        }

        @Override
        @SneakyThrows
        public void init(TablesStorageInit init) {
            SQLDataStorageService storage = testDataStorages.getTestDataStorage(init.getName(), SQLDataStorageService.class);

            List<String> sqls = new ArrayList<>();
            if (init.getSqlFilePath() != null) {
                for (String sqlFile : init.getSqlFilePath()) {
                    sqls.add(new String(new ClassPathResource(sqlFile).getInputStream().readAllBytes(), StandardCharsets.UTF_8));
                }
            }
            if (init.getSql() != null) {
                sqls.addAll(init.getSql());
            }
            storage.prepareData(sqls, stringToList(init.getTablesToChange()), stringToList(init.getTablesToLoad()),
                    init.getLoadAllTables() != null && init.getLoadAllTables(),
                    init.getDisableCacheReload() != null && init.getDisableCacheReload());
            testDataStorages.getMapDiff();
        }

        private Set<String> stringToList(String string) {
            if (string == null || string.isEmpty()) {
                return null;
            }
            return Arrays.stream(string.split(","))
                    .map(String::strip)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }
}