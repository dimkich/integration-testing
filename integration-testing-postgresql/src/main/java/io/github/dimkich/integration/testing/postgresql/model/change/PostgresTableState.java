package io.github.dimkich.integration.testing.postgresql.model.change;

import io.github.dimkich.integration.testing.postgresql.model.metadata.PostgresTableMetadata;
import io.github.dimkich.integration.testing.storage.sql.state.MutationImpact;
import io.github.dimkich.integration.testing.storage.sql.state.StorageMutation;
import io.github.dimkich.integration.testing.storage.sql.table.Table;
import io.github.dimkich.integration.testing.storage.sql.table.TableRegistry;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Initial and final state of one audit table inside one scope. Journal records are folded into one
 * {@link PostgresRowState} per raw row key, so the same state is used by the difference engine and
 * by the Smart Revert.
 * <p>
 * The table state is mutable only while the change batch is being built: {@link #addRecord} appends
 * a journal record, then {@link #getRows} builds the endpoint state lazily. Only the first before
 * image and the last after image of a key are kept. A key that was inserted and then deleted has no
 * state at all. This is a state diff, not a history of physical rows: a primary-key change produces
 * one state for the old key and one for the new key.
 */
@Getter
public final class PostgresTableState {
    private final Table table;
    @Getter(AccessLevel.NONE)
    private final List<PostgresJournalRecord> records = new ArrayList<>();
    @Getter(AccessLevel.NONE)
    private final PostgresTableMetadata metadata;
    @Getter(AccessLevel.NONE)
    private List<PostgresRowState> rowList;
    @Getter(AccessLevel.NONE)
    private List<PostgresRowState> changedRows;
    private int maxUndoRows;

    public PostgresTableState(Table table) {
        this.table = table;
        this.metadata = table.getMetadata(PostgresTableMetadata.class);
    }

    /**
     * Appends a journal record while the change batch is being built.
     */
    public void addRecord(PostgresJournalRecord record) {
        if (rowList != null) {
            throw new IllegalStateException("Cannot add records after rows have been built");
        }
        records.add(record);
        int weight = record.undoWeight();
        maxUndoRows = maxUndoRows > Integer.MAX_VALUE - weight
                ? Integer.MAX_VALUE
                : maxUndoRows + weight;
    }

    /**
     * Net state of every key touched by the batch, in first-seen order.
     */
    public List<PostgresRowState> getRows() {
        if (rowList == null) {
            rowList = buildRows();
        }
        return rowList;
    }

    /**
     * Rows whose logical state changed during the batch.
     */
    public List<PostgresRowState> getChangedRows() {
        if (changedRows == null) {
            List<PostgresRowState> rows = getRows();
            List<PostgresRowState> result = new ArrayList<>(rows.size());
            for (PostgresRowState row : rows) {
                if (row.isChanged()) {
                    result.add(row);
                }
            }
            changedRows = result;
        }
        return changedRows;
    }

    /**
     * Builds the storage mutation of this table state.
     */
    public StorageMutation toMutation(TableRegistry registry, MutationImpact impact) {
        return StorageMutation.builder(registry)
                .impact(table, impact)
                .build();
    }

    /**
     * Exact number of physical row operations needed to revert this table state.
     */
    public int getUndoRowCount() {
        int result = 0;
        for (PostgresRowState row : getRows()) {
            result += row.getUndoRowCount();
        }
        return result;
    }

    private List<PostgresRowState> buildRows() {
        int expected = records.size();
        Map<PostgresRawKey, PostgresRawRow> before = new HashMap<>(expected);
        Map<PostgresRawKey, PostgresRawRow> after = new HashMap<>(expected);
        Set<PostgresRawKey> order = new LinkedHashSet<>(expected);
        for (PostgresJournalRecord record : records) {
            PostgresRawRow oldRow = record.getOldRow();
            if (oldRow != null) {
                PostgresRawKey key = oldRow.getRowKey();
                if (order.add(key)) {
                    before.put(key, oldRow);
                }
                after.remove(key);
            }
            PostgresRawRow newRow = record.getNewRow();
            if (newRow != null) {
                PostgresRawKey key = newRow.getRowKey();
                order.add(key);
                after.put(key, newRow);
            }
        }

        List<PostgresRowState> result = new ArrayList<>(order.size());
        for (PostgresRawKey key : order) {
            PostgresRawRow beforeRow = before.get(key);
            PostgresRawRow afterRow = after.get(key);
            if (beforeRow != null || afterRow != null) {
                result.add(new PostgresRowState(metadata, beforeRow, afterRow));
            }
        }
        return result;
    }
}
