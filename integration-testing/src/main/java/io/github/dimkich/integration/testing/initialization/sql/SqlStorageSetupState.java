package io.github.dimkich.integration.testing.initialization.sql;

import io.github.dimkich.integration.testing.initialization.TestInitState;
import io.github.dimkich.integration.testing.storage.sql.SQLDataStorageService;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.*;

@Getter
@ToString
@NoArgsConstructor
public class SqlStorageSetupState implements TestInitState<SqlStorageSetupState> {
    private final Map<SQLDataStorageService, InitData> initData = new HashMap<>();

    SqlStorageSetupState(SQLDataStorageService storage, InitData initData) {
        this.initData.put(storage, initData);
    }

    @Override
    public SqlStorageSetupState merge(SqlStorageSetupState state) {
        initData.putAll(state.initData);
        return this;
    }

    @Override
    public SqlStorageSetupState copy() {
        SqlStorageSetupState copy = new SqlStorageSetupState();
        copy.initData.putAll(this.initData);
        return copy;
    }

    @Data
    static class InitData {
        private List<String> sql = new ArrayList<>();
        private Set<String> dbUnitPath = new HashSet<>();
        private Set<SqlStorageSetup.TableHook> tableHook = new LinkedHashSet<>();
    }
}
