package io.github.dimkich.integration.testing.postgresql.model.revert;

import io.github.dimkich.integration.testing.postgresql.model.metadata.PostgresTableMetadata;
import io.github.dimkich.integration.testing.storage.sql.table.Table;
import lombok.RequiredArgsConstructor;

/**
 * Renders a DELETE that matches rows by primary key or by the complete row when the table has no
 * primary key.
 */
@RequiredArgsConstructor
public final class DeleteRevertStatement implements PostgresRevertStatement {
    private final Table table;
    private final PostgresRevertValues values;

    @Override
    public String toSql() {
        PostgresTableMetadata metadata = table.getMetadata(PostgresTableMetadata.class);
        return "DELETE FROM " + table.getQualified() + " AS t USING "
                + values.toSql() + " WHERE " + metadata.deleteMatch();
    }
}
