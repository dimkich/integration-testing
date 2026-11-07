package io.github.dimkich.integration.testing.initialization.sql;

import io.github.dimkich.integration.testing.Test;
import io.github.dimkich.integration.testing.initialization.InitSetup;
import io.github.dimkich.integration.testing.storage.TestDataStorages;
import io.github.dimkich.integration.testing.storage.sql.SQLDataStorageService;
import io.github.dimkich.integration.testing.storage.sql.state.StorageState;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public class SqlStorageInitSetup implements InitSetup<SqlStorageInit, SqlStorageInitState> {
    private final TestDataStorages testDataStorages;

    @Override
    public Class<SqlStorageInit> getTestCaseInitClass() {
        return SqlStorageInit.class;
    }

    @Override
    public SqlStorageInitState defaultState() {
        return testDataStorages.getTestDataStorages(SQLDataStorageService.class)
                .map(s -> new SqlStorageInitState(s, s.createDefaultState()))
                .reduce(SqlStorageInitState::merge)
                .orElseGet(SqlStorageInitState::new);
    }

    @Override
    public SqlStorageInitState convert(SqlStorageInit init) {
        SQLDataStorageService storage = testDataStorages.getTestDataStorage(init.getName(),
                SQLDataStorageService.class);
        return new SqlStorageInitState(storage, storage.createFromInitState(init));
    }

    @Override
    public void apply(SqlStorageInitState oldState, SqlStorageInitState newState, Test test) throws Exception {
        for (Map.Entry<SQLDataStorageService, StorageState> entry : newState.getEntries()) {
            SQLDataStorageService storage = entry.getKey();
            storage.applyChanges(oldState.getStorageState(storage), entry.getValue());
        }
    }

    @Override
    public Integer getOrder() {
        return 2000;
    }
}
