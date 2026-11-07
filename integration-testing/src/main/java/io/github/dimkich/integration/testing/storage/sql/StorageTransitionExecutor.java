package io.github.dimkich.integration.testing.storage.sql;

import io.github.dimkich.integration.testing.dbunit.DbUnitFixture;
import io.github.dimkich.integration.testing.storage.sql.state.TransitionPlan;
import io.github.dimkich.integration.testing.storage.sql.table.TableSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Executes one transition plan. Fixture loading is kept here because it has to happen between the
 * rollback/cleanup statements and the forward SQL.
 */
@Slf4j
@RequiredArgsConstructor
public final class StorageTransitionExecutor {
    private final SQLDataStorage storage;
    private final DbUnitFixture fixture;

    void execute(TransitionPlan plan) throws Exception {
        TableSet tablesToLoad = plan.getTablesToLoad();
        TableSet tablesToClear = plan.getTablesToClear();
        List<String> forwardSql = plan.getForwardSql();

        List<String> cleanupSql = plan.getRollbackSql();
        if (!tablesToClear.isEmpty()) {
            cleanupSql = new ArrayList<>(cleanupSql);
            cleanupSql.add(storage.getClearSql(tablesToClear));
        }

        if (tablesToLoad.isEmpty() || !fixture.isConfigured()) {
            // Rollback, clearing and the tagged forward SQL are one database request. A storage
            // without a fixture simply has nothing to load.
            if (!cleanupSql.isEmpty() || !forwardSql.isEmpty()) {
                storage.executeSql(cleanupSql, forwardSql);
            }
            return;
        }

        // Fixtures have to be loaded after the old state is removed and before the target SQL is
        // executed, so this path cannot merge all three phases into one request.
        if (!cleanupSql.isEmpty()) {
            storage.executeSql(cleanupSql, List.of());
        }
        log.debug("Storage '{}' load fixtures: {}", storage.getName(), tablesToLoad);
        storage.loadFixture(fixture, tablesToLoad);
        if (!forwardSql.isEmpty()) {
            storage.executeSql(List.of(), forwardSql);
        }
    }
}
