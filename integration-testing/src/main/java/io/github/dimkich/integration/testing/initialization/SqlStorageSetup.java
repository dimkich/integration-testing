package io.github.dimkich.integration.testing.initialization;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dimkich.integration.testing.TestCaseInit;
import io.github.dimkich.integration.testing.storage.TestDataStorages;
import io.github.dimkich.integration.testing.storage.sql.SQLDataStorageService;
import lombok.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Getter
@Setter
@ToString
public class SqlStorageSetup extends TestCaseInit {
    @JacksonXmlProperty(isAttribute = true)
    private String name;
    private List<String> sqlFilePath;
    private List<String> sql;
    private Set<String> dbUnitPath;
    private Set<TableHook> tableHook;

    @Data
    public static class TableHook {
        @JacksonXmlProperty(isAttribute = true)
        private String tableName;
        @JacksonXmlProperty(isAttribute = true)
        private String beanName;
        @JacksonXmlProperty(isAttribute = true)
        private String beanMethod;
    }

    @Override
    public Integer getOrder() {
        return 1000;
    }

    @Component
    @RequiredArgsConstructor
    public static class Initializer implements TestCaseInitializer<SqlStorageSetup> {
        private final TestDataStorages testDataStorages;

        @Override
        public Class<SqlStorageSetup> getTestCaseInitClass() {
            return SqlStorageSetup.class;
        }

        @Override
        @SneakyThrows
        public void init(SqlStorageSetup init) {
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
            storage.executeSqls(sqls);

            if (init.getDbUnitPath() != null) {
                storage.setDbUnitXml(init.getDbUnitPath());
            }
            if (init.getTableHook() != null) {
                storage.setTableHooks(init.getTableHook());
            }
        }
    }
}
