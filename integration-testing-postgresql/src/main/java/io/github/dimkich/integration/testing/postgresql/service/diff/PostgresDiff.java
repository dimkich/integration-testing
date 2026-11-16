package io.github.dimkich.integration.testing.postgresql.service.diff;

import io.github.dimkich.integration.testing.postgresql.model.change.PostgresChangeBatch;
import io.github.dimkich.integration.testing.postgresql.model.change.PostgresRowState;
import io.github.dimkich.integration.testing.postgresql.model.change.PostgresTableState;
import io.github.dimkich.integration.testing.postgresql.model.metadata.PostgresRowValues;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Net diff of one change batch: the logical rows before and after the recorded changes.
 * <p>
 * The row maps are unordered, so the difference engine sorts the records by key. An ordered map
 * would keep the order of changes instead.
 */
@Getter
final class PostgresDiff {
    private final Map<String, Object> before = new HashMap<>();
    private final Map<String, Object> after = new HashMap<>();

    static PostgresDiff from(PostgresChangeBatch batch) {
        PostgresDiff diff = new PostgresDiff();
        batch.getSutTables().forEach(diff::addTable);
        return diff;
    }

    private void addTable(PostgresTableState table) {
        String tableName = table.getTable().getCanonicalName();
        for (PostgresRowState row : table.getChangedRows()) {
            PostgresRowValues values = row.getValues();
            if (values.hasOldData()) {
                putBefore(tableName, row.getBeforeDisplayKey(), values.getOldData());
            }
            if (values.hasNewData()) {
                putAfter(tableName, row.getAfterDisplayKey(), values.getNewData());
            }
        }
    }

    private void putBefore(String tableName, String key, Map<String, Object> value) {
        putUnique(before, tableName, key, value, "before");
    }

    private void putAfter(String tableName, String key, Map<String, Object> value) {
        putUnique(after, tableName, key, value, "after");
    }

    private void putUnique(Map<String, Object> target, String tableName, String key,
                           Map<String, Object> value, String side) {
        Object previous = target.putIfAbsent(key, value);
        if (previous != null) {
            throw new IllegalStateException("Duplicate " + side + " row key '" + key
                    + "' for table " + tableName);
        }
    }
}
