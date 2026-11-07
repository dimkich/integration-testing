package io.github.dimkich.integration.testing.storage.sql.state;

import io.github.dimkich.integration.testing.storage.sql.table.Table;
import io.github.dimkich.integration.testing.storage.sql.table.TableRegistry;
import io.github.dimkich.integration.testing.storage.sql.table.TableSet;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * Ready plan that moves a storage from its current state to the target state.
 */
@Getter
@ToString
public class TransitionPlan {
    private final TableSet tablesToLoad;
    private final TableSet tablesToClear;
    private final TableSet changedTables;
    private final List<String> rollbackSql = new ArrayList<>();
    private final List<String> forwardSql = new ArrayList<>();

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.PACKAGE)
    @ToString.Exclude
    private StorageMutationLog storageMutationLog;

    public TransitionPlan(TableRegistry registry) {
        this.tablesToLoad = TableSet.empty(registry);
        this.tablesToClear = TableSet.empty(registry);
        this.changedTables = TableSet.empty(registry);
    }

    public boolean hasChanges() {
        return !forwardSql.isEmpty() || !tablesToLoad.isEmpty() || !tablesToClear.isEmpty()
                || !rollbackSql.isEmpty();
    }

    public void load(Table table) {
        tablesToLoad.add(table);
        tablesToClear.remove(table);
        changedTables.add(table);
    }

    public void clear(Table table) {
        tablesToClear.add(table);
        tablesToLoad.remove(table);
        changedTables.add(table);
    }

    public void markChanged(Table table) {
        changedTables.add(table);
    }

    public void addRollbackSql(List<String> sql) {
        rollbackSql.addAll(sql);
    }

    public void addForwardSql(String sql) {
        forwardSql.add(sql);
    }

    public void applyMutationLog(StorageState target) {
        target.setStorageMutationLog(storageMutationLog);
    }
}
