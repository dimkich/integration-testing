package io.github.dimkich.integration.testing.postgresql.model.revert;

import io.github.dimkich.integration.testing.postgresql.model.metadata.PostgresTableMetadata;
import io.github.dimkich.integration.testing.storage.sql.table.Table;
import lombok.RequiredArgsConstructor;

/**
 * Renders a plain UPDATE that matches rows by primary key.
 */
@RequiredArgsConstructor
public final class UpdateRevertStatement implements PostgresRevertStatement {
    private final Table table;
    private final PostgresRevertValues values;

    @Override
    public String toSql() {
        PostgresTableMetadata metadata = table.getMetadata(PostgresTableMetadata.class);
        return "UPDATE " + table.getQualified() + " AS t SET " + metadata.getUpdateAssignmentsSql()
                + " FROM " + values.toSql()
                + " WHERE " + metadata.primaryKeyMatch();
    }
}
