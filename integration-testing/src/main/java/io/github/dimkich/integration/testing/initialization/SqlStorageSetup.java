package io.github.dimkich.integration.testing.initialization;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dimkich.integration.testing.TestCaseInit;
import io.github.dimkich.integration.testing.storage.TestDataStorages;
import io.github.dimkich.integration.testing.storage.sql.SQLDataStorageService;
import lombok.*;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

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

    @RequiredArgsConstructor
    public static class Init implements Initializer<SqlStorageSetup> {
        private final TestDataStorages testDataStorages;

        @Override
        public Class<SqlStorageSetup> getTestCaseInitClass() {
            return SqlStorageSetup.class;
        }

        @Override
        public Integer getOrder() {
            return 1000;
        }

        @Override
        public void init(Collection<SqlStorageSetup> inits) throws Exception {
            for (SqlStorageSetup init : inits) {
                SQLDataStorageService storage = testDataStorages.getTestDataStorage(init.getName(),
                        SQLDataStorageService.class);

                List<String> sqls = new ArrayList<>();
                if (init.getSqlFilePath() != null) {
                    for (String sqlFile : init.getSqlFilePath()) {
                        sqls.add(new String(new ClassPathResource(sqlFile).getInputStream().readAllBytes(),
                                StandardCharsets.UTF_8));
                    }
                }
                if (init.getSql() != null) {
                    sqls.addAll(init.getSql());
                }
                if (!sqls.isEmpty()) {
                    storage.executeSqls(sqls);
                    testDataStorages.addAffectedStorage(storage);
                }

                if (init.getDbUnitPath() != null) {
                    storage.setDbUnitXml(init.getDbUnitPath());
                }
                if (init.getTableHook() != null) {
                    storage.setTableHooks(init.getTableHook());
                }
            }
        }
    }
}
