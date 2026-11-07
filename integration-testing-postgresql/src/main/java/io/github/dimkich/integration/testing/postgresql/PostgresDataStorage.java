package io.github.dimkich.integration.testing.postgresql;

import io.github.dimkich.integration.testing.dbunit.DbUnitFixture;
import io.github.dimkich.integration.testing.postgresql.model.metadata.PostgresTableMetadata;
import io.github.dimkich.integration.testing.postgresql.repository.PostgresTableCatalogRepository;
import io.github.dimkich.integration.testing.postgresql.repository.PostgresTypeCatalogRepository;
import io.github.dimkich.integration.testing.postgresql.service.PostgresTableMetadataService;
import io.github.dimkich.integration.testing.postgresql.service.change.PostgresChangePipeline;
import io.github.dimkich.integration.testing.postgresql.service.journal.PostgresJournalLifecycle;
import io.github.dimkich.integration.testing.storage.sql.SQLDataStorage;
import io.github.dimkich.integration.testing.storage.sql.StorageChanges;
import io.github.dimkich.integration.testing.storage.sql.table.Table;
import io.github.dimkich.integration.testing.storage.sql.table.TableRegistry;
import io.github.dimkich.integration.testing.storage.sql.table.TableSet;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collection;

/**
 * PostgreSQL {@link SQLDataStorage} facade. The JDBC, journal, change, diff and DBUnit concerns
 * live in dedicated service and infrastructure components behind this class.
 */
@RequiredArgsConstructor
public class PostgresDataStorage implements SQLDataStorage {
    // The order of the fields is coupled to the bean definition in PostgresConfig.PostProcessor.
    @Getter
    private final String name;
    private final TableRegistry registry;
    private final PostgresDatabase database;
    private final PostgresTableCatalogRepository tableCatalogRepository;
    private final PostgresTypeCatalogRepository typeCatalogRepository;
    private final PostgresJournalLifecycle journalLifecycle;
    private final PostgresTableMetadataService metadataService;
    private final PostgresChangePipeline changePipeline;

    @Override
    public void initialize() throws Exception {
        typeCatalogRepository.reset();
        tableCatalogRepository.reset();
        metadataService.reset();
        journalLifecycle.resetJournal();
        changePipeline.reset();
        database.resetFixture();
        registry.updateTables(tableCatalogRepository.getTables());
        TableSet tables = registry.getCurrentTables();
        metadataService.prepare(tables);
        journalLifecycle.attachTriggers(tables);
    }

    @Override
    public void executeSql(Collection<String> untrackedSql, Collection<String> trackedSql)
            throws Exception {
        String[] taggedSql = trackedSql.toArray(String[]::new);
        if (taggedSql.length > 0) {
            changePipeline.assertNoPendingTaggedSql();
        }
        database.executeTransition(untrackedSql, taggedSql);
        changePipeline.setTaggedSql(taggedSql);
    }

    @Override
    public void loadFixture(DbUnitFixture fixture, TableSet tables) throws Exception {
        database.loadFixture(fixture, tables);
    }

    @Override
    public String getClearSql(TableSet tables) {
        StringBuilder sql = new StringBuilder(Math.max(16, tables.size() * 32));
        for (Table table : tables) {
            if (!sql.isEmpty()) {
                sql.append(";\n");
            }
            sql.append("DELETE FROM ").append(table.getQualified());
        }
        return sql.toString();
    }

    @Override
    public String getSequenceRestartSql() {
        StringBuilder sql = new StringBuilder();
        for (Table table : registry.getCurrentTables()) {
            for (String statement : table.getMetadata(PostgresTableMetadata.class).getSequenceRestartSql()) {
                if (!sql.isEmpty()) {
                    sql.append(";\n");
                }
                sql.append(statement);
            }
        }
        return sql.toString();
    }

    @Override
    public StorageChanges getStorageChanges(String postReadSql) throws Exception {
        return changePipeline.getStorageChanges(registry, postReadSql);
    }
}
