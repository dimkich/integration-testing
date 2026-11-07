package io.github.dimkich.integration.testing.postgresql.model.change;

import io.github.dimkich.integration.testing.storage.sql.table.TableMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * One read of the PostgreSQL audit journal. The batch keeps the raw records for tagged SQL and
 * lazily folds the SUT records into the initial and final state of every touched table, so the
 * difference and the Smart Revert share the same state.
 */
@Getter
@RequiredArgsConstructor
public final class PostgresChangeBatch {
    private final List<PostgresJournalRecord> records;
    private TableMap<PostgresTableState> sutTables;

    /**
     * SUT table states in table id order. The returned states are shared by the difference and the
     * revert impact, including their lazily built row states.
     */
    public TableMap<PostgresTableState> getSutTables() {
        if (sutTables == null) {
            sutTables = new TableMap<>();
            for (PostgresJournalRecord record : records) {
                if (record.isSut()) {
                    sutTables.computeIfAbsent(record.getTable(), PostgresTableState::new)
                            .addRecord(record);
                }
            }
        }
        return sutTables;
    }
}
