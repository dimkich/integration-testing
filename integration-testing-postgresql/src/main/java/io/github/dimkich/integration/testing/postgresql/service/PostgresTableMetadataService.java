package io.github.dimkich.integration.testing.postgresql.service;

import io.github.dimkich.integration.testing.postgresql.model.metadata.PostgresTableMetadata;
import io.github.dimkich.integration.testing.postgresql.repository.PostgresTableMetadataRepository;
import io.github.dimkich.integration.testing.storage.sql.table.Table;
import io.github.dimkich.integration.testing.storage.sql.table.TableSet;
import lombok.RequiredArgsConstructor;

import java.sql.SQLException;

/**
 * Owns the PostgreSQL metadata attached to canonical tables. Preparation is a single bulk pass for
 * the whole table set; hot-path readers only read the already attached metadata.
 */
@RequiredArgsConstructor
public class PostgresTableMetadataService {
    // The order of the fields is coupled to the bean definition in PostgresConfig.PostProcessor.
    private final PostgresTableMetadataRepository tableMetadataRepository;

    private TableSet preparedTables;

    public void prepare(TableSet tables) throws SQLException {
        if (tables.isEmpty()) {
            return;
        }
        if (preparedTables == null) {
            preparedTables = TableSet.empty(tables.getRegistry());
        }
        boolean prepared = true;
        for (Table table : tables) {
            if (!table.hasMetadata()) {
                prepared = false;
                break;
            }
        }
        if (prepared) {
            return;
        }

        for (PostgresTableMetadata metadata : tableMetadataRepository.load(tables)) {
            Table table = metadata.getTable();
            table.setMetadata(metadata);
            preparedTables.add(table);
        }
    }

    public void reset() {
        if (preparedTables == null) {
            return;
        }
        for (Table table : preparedTables) {
            table.clearMetadata();
        }
        preparedTables.clear();
    }
}
