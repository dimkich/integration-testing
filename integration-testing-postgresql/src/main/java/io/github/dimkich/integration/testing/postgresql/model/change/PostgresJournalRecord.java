package io.github.dimkich.integration.testing.postgresql.model.change;

import io.github.dimkich.integration.testing.storage.sql.table.Table;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * One row of the internal PostgreSQL audit journal table. The record owns the old and new tuple
 * images and exposes the journal action; tuple parsing remains lazy and starts only when a row key
 * or the tuple text is requested.
 */
@Getter
@RequiredArgsConstructor
public final class PostgresJournalRecord {
    private final long id;
    private final Table table;
    private final PostgresJournalAction action;
    private final PostgresRawRow oldRow;
    private final PostgresRawRow newRow;
    private final Integer node;

    /**
     * A SUT record was produced outside an SQL transition and therefore has no correlation tag.
     */
    public boolean isSut() {
        return node == null;
    }

    public boolean isTagged() {
        return node != null;
    }

    public boolean isTaggedWith(int tag) {
        return node != null && node == tag;
    }

    /**
     * Physical row operations needed to roll back this record. An update can require a delete plus
     * an insert when the primary key or an identity-always column changes.
     */
    public int undoWeight() {
        return switch (action) {
            case UPDATE -> 2;
            case INSERT, DELETE -> 1;
        };
    }
}
