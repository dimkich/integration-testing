package io.github.dimkich.integration.testing.postgresql.model.revert;

import io.github.dimkich.integration.testing.postgresql.model.change.PostgresRowState;
import io.github.dimkich.integration.testing.postgresql.model.change.PostgresTableState;
import io.github.dimkich.integration.testing.postgresql.model.metadata.PostgresTableMetadata;
import io.github.dimkich.integration.testing.storage.sql.table.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * Reusable renderer for all revert commands of one table. The instance is reset before every
 * generation, so its builders keep their capacity across tables.
 * <p>
 * Commands are built from the initial and final state of every raw row key. The operation groups
 * are disjoint by key: a key is inserted, updated, reinserted or deleted, never two of them at once.
 * The command order below is therefore safe; it does not depend on the order in which the original
 * journal records were applied.
 */
public final class PostgresRevertTableSql {
    private final PostgresRevertValues insertValues = new PostgresRevertValues();
    private final PostgresRevertValues updateValues = new PostgresRevertValues();
    private final PostgresRevertValues reinsertDeleteValues = new PostgresRevertValues();
    private final PostgresRevertValues reinsertInsertValues = new PostgresRevertValues();
    private final PostgresRevertValues deleteValues = new PostgresRevertValues();

    private Table table;

    private void reset(Table table) {
        this.table = table;
        insertValues.reset();
        updateValues.reset();
        reinsertDeleteValues.reset();
        reinsertInsertValues.reset();
        deleteValues.reset();
    }

    private void addRowState(PostgresRowState row) {
        switch (row.getChange()) {
            case INSERT -> deleteValues.add(row.getAfter());
            case DELETE -> insertValues.add(row.getBefore());
            case UNCHANGED -> {
            }
            case REINSERT -> {
                reinsertDeleteValues.add(row.getAfter());
                reinsertInsertValues.add(row.getBefore());
            }
            case UPDATE -> updateValues.add(row.getBefore());
        }
    }

    public List<String> render(PostgresTableState state) {
        reset(state.getTable());
        for (PostgresRowState row : state.getChangedRows()) {
            addRowState(row);
        }
        return toSql();
    }

    private List<String> toSql() {
        PostgresTableMetadata metadata = table.getMetadata(PostgresTableMetadata.class);
        List<String> commands = new ArrayList<>(5);
        if (insertValues.hasValues()) {
            commands.add(new InsertRevertStatement(table, insertValues).toSql());
        }
        if (updateValues.hasValues() && metadata.hasUpdateColumns()) {
            commands.add(new UpdateRevertStatement(table, updateValues).toSql());
        }
        if (reinsertDeleteValues.hasValues()) {
            commands.add(new DeleteRevertStatement(table, reinsertDeleteValues).toSql());
            commands.add(new InsertRevertStatement(table, reinsertInsertValues).toSql());
        }
        if (deleteValues.hasValues()) {
            commands.add(new DeleteRevertStatement(table, deleteValues).toSql());
        }
        return commands;
    }
}
