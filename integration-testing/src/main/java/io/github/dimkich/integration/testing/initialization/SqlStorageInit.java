package io.github.dimkich.integration.testing.initialization;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dimkich.integration.testing.TestCaseInit;
import io.github.dimkich.integration.testing.storage.TestDataStorages;
import io.github.dimkich.integration.testing.storage.sql.SQLDataStorageService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@ToString
public class SqlStorageInit extends TestCaseInit {
    @JacksonXmlProperty(isAttribute = true)
    private String name;
    @JacksonXmlProperty(isAttribute = true)
    private Boolean loadAllTables;
    @JacksonXmlProperty(isAttribute = true)
    private Boolean disableTableHooks;
    private String tablesToChange;
    private String tablesToLoad;
    private List<String> sql;

    @RequiredArgsConstructor
    public static class Init implements Initializer<SqlStorageInit> {
        private final TestDataStorages testDataStorages;

        private final Set<SQLDataStorageService> set = new LinkedHashSet<>();

        @Override
        public Class<SqlStorageInit> getTestCaseInitClass() {
            return SqlStorageInit.class;
        }

        @Override
        public Integer getOrder() {
            return 2000;
        }

        @Override
        public void init(Collection<SqlStorageInit> inits) throws Exception {
            set.clear();
            for (SqlStorageInit init : inits) {
                SQLDataStorageService service = testDataStorages.getTestDataStorage(init.getName(),
                        SQLDataStorageService.class);
                service.addInit(init);
                set.add(service);
            }
            for (SQLDataStorageService storage : set) {
                if (storage.applyChanges()) {
                    testDataStorages.addAffectedStorage(storage);
                }
            }
        }
    }
}