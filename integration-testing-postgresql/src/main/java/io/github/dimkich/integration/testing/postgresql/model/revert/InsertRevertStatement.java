package io.github.dimkich.integration.testing.postgresql.model.revert;

import io.github.dimkich.integration.testing.postgresql.model.metadata.PostgresTableMetadata;
import io.github.dimkich.integration.testing.storage.sql.table.Table;
import lombok.RequiredArgsConstructor;

/**
 * Renders the INSERT of rows restored from a typed VALUES source.
 */
@RequiredArgsConstructor
public final class InsertRevertStatement implements PostgresRevertStatement {
    private final Table table;
    private final PostgresRevertValues values;

    @Override
    public String toSql() {
        PostgresTableMetadata metadata = table.getMetadata(PostgresTableMetadata.class);
        String overriding = metadata.getIdentityAlways().isEmpty()
                ? " " : " OVERRIDING SYSTEM VALUE ";
        return "INSERT INTO " + table.getQualified() + " (" + metadata.getInsertColumnsSql() + ")"
                + overriding + "SELECT " + metadata.getInsertSelectSql() + " FROM " + values.toSql();
    }
}
