package io.github.dimkich.integration.testing.initialization;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import eu.ciechanowiec.sneakyfun.SneakyBiConsumer;
import eu.ciechanowiec.sneakyfun.SneakyBiFunction;
import eu.ciechanowiec.sneakyfun.SneakyConsumer;
import io.github.dimkich.integration.testing.TestInit;
import io.github.dimkich.integration.testing.storage.TestDataStorages;
import io.github.dimkich.integration.testing.storage.sql.SQLDataStorageService;
import lombok.*;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;

@Getter
@Setter
@ToString
public class SqlStorageSetup extends TestInit {
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
        private final Map<SQLDataStorageService, InitData> initData = new LinkedHashMap<>();

        @Override
        public Class<SqlStorageSetup> getTestInitClass() {
            return SqlStorageSetup.class;
        }

        @Override
        public Integer getOrder() {
            return 1000;
        }

        @Override
        public void init(Stream<SqlStorageSetup> inits) throws Exception {
            initData.clear();
            inits.forEach(SneakyConsumer.sneaky(init -> {
                SQLDataStorageService storage = testDataStorages.getTestDataStorage(init.getName(),
                        SQLDataStorageService.class);
                initData.compute(storage, SneakyBiFunction.sneaky((s, data) -> {
                    if (data == null) {
                        data = new InitData();
                    }
                    if (init.getSqlFilePath() != null) {
                        for (String sqlFile : init.getSqlFilePath()) {
                            data.getSql().add(new String(new ClassPathResource(sqlFile).getInputStream().readAllBytes(),
                                    StandardCharsets.UTF_8));
                        }
                    }
                    if (init.getSql() != null) {
                        data.getSql().addAll(init.getSql());
                    }
                    if (init.getDbUnitPath() != null) {
                        data.getDbUnitPath().addAll(init.getDbUnitPath());
                    }
                    if (init.getTableHook() != null) {
                        data.getTableHook().addAll(init.getTableHook());
                    }
                    return data;
                }));
            }));
            initData.forEach(SneakyBiConsumer.sneaky((s, data) -> {
                if (!data.getSql().isEmpty()) {
                    s.executeSqls(data.getSql());
                    testDataStorages.addAffectedStorage(s);
                }
                s.setDbUnitXml(data.getDbUnitPath());
                s.setTableHooks(data.getTableHook());
                s.init();
            }));
        }

        @Data
        private static class InitData {
            private List<String> sql = new ArrayList<>();
            private Set<String> dbUnitPath = new HashSet<>();
            private Set<TableHook> tableHook = new HashSet<>();
        }
    }
}
