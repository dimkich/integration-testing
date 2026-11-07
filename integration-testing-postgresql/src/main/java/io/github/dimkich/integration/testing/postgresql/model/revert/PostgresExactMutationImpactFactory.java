package io.github.dimkich.integration.testing.postgresql.model.revert;

import io.github.dimkich.integration.testing.postgresql.model.change.PostgresTableState;
import io.github.dimkich.integration.testing.storage.sql.state.ExactMutationImpact;

import java.util.List;

/**
 * Creates the exact mutation impact of one table. The table state is shared with the diff; the
 * rollback cost is counted immediately, while SQL is rendered lazily from the same state.
 */
public class PostgresExactMutationImpactFactory {
    private static final ExactMutationImpact EMPTY = new ExactMutationImpact(0, List.of());

    private final PostgresRevertTableSql tableSql = new PostgresRevertTableSql();

    public ExactMutationImpact create(PostgresTableState state) {
        int undoRows = state.getUndoRowCount();
        if (undoRows == 0) {
            return EMPTY;
        }
        return new ExactMutationImpact(undoRows, () -> tableSql.render(state));
    }
}
