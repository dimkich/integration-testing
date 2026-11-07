package io.github.dimkich.integration.testing.storage.sql;

import io.github.dimkich.integration.testing.storage.sql.table.Table;
import io.github.dimkich.integration.testing.storage.sql.table.TableMap;
import io.github.dimkich.integration.testing.storage.sql.table.TableSet;

/**
 * Tracks the database revision caused by framework changes for every table and the revision
 * acknowledged by the application cache. SUT changes are intentionally not tracked here: the SUT
 * knows its own changes, while framework changes must be delivered to it.
 */
public final class TableRevisionTracker {
    private final TableMap<Long> databaseRevisions = new TableMap<>();
    private final TableMap<Long> acknowledgedRevisions = new TableMap<>();

    void increment(TableSet tables) {
        tables.forEachTable(table -> databaseRevisions.put(table,
                revision(databaseRevisions, table) + 1));
    }

    boolean isStale(Table table) {
        return revision(databaseRevisions, table) > revision(acknowledgedRevisions, table);
    }

    void acknowledge(Table table) {
        acknowledgedRevisions.put(table, revision(databaseRevisions, table));
    }

    void reset() {
        databaseRevisions.clear();
        acknowledgedRevisions.clear();
    }

    private static long revision(TableMap<Long> revisions, Table table) {
        Long revision = revisions.get(table);
        return revision == null ? 0L : revision;
    }
}
