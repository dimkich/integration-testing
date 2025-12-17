package io.github.dimkich.integration.testing.initialization.sql;

import eu.ciechanowiec.sneakyfun.SneakyBiConsumer;
import io.github.dimkich.integration.testing.Test;
import io.github.dimkich.integration.testing.initialization.InitSetup;
import io.github.dimkich.integration.testing.storage.TestDataStorages;
import io.github.dimkich.integration.testing.storage.sql.SQLDataStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class SqlStorageNoHookInitSetup implements InitSetup<SqlStorageNoHookInit, SqlStorageNoHookInitState> {
    private final TestDataStorages testDataStorages;

    @Override
    public Class<SqlStorageNoHookInit> getTestCaseInitClass() {
        return SqlStorageNoHookInit.class;
    }

    @Override
    public SqlStorageNoHookInitState defaultState() {
        return new SqlStorageNoHookInitState();
    }

    @Override
    public SqlStorageNoHookInitState convert(SqlStorageNoHookInit init) throws Exception {
        SQLDataStorageService storage = testDataStorages.getTestDataStorage(init.getName(),
                SQLDataStorageService.class);
        return new SqlStorageNoHookInitState(storage, init.getSql());
    }

    @Override
    public void apply(SqlStorageNoHookInitState oldState, SqlStorageNoHookInitState newState, Test test) throws Exception {
        newState.foreach(SneakyBiConsumer.sneaky(((storage, sql) -> {
            log.debug("Init '{}' no hook SQL: {}", storage.getName(), sql);
            storage.executeSqls(sql);
        })));
    }

    @Override
    public boolean saveState() {
        return false;
    }

    @Override
    public Integer getOrder() {
        return 11000;
    }
}
