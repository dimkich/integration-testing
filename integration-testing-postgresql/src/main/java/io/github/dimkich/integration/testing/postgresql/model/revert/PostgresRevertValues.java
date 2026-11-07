package io.github.dimkich.integration.testing.postgresql.model.revert;

import io.github.dimkich.integration.testing.postgresql.model.change.PostgresRawRow;

/**
 * Reusable accumulator for typed row tuples. The accumulator keeps its capacity between calls, so
 * repeated revert rendering does not grow a new character array for every table.
 */
public final class PostgresRevertValues {
    private final StringBuilder tuples = new StringBuilder();

    public void reset() {
        tuples.setLength(0);
    }

    public void add(PostgresRawRow row) {
        if (!tuples.isEmpty()) {
            tuples.append(", ");
        }
        tuples.append('(').append(row.toSqlTuple()).append(')');
    }

    public boolean hasValues() {
        return !tuples.isEmpty();
    }

    public String toSql() {
        return "(VALUES " + tuples + ") v(r)";
    }
}
