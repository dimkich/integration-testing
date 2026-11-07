package io.github.dimkich.integration.testing.postgresql.repository;

import io.github.dimkich.integration.testing.date.time.DateTimeService;
import io.github.dimkich.integration.testing.postgresql.config.PostgresProperties;
import io.github.dimkich.integration.testing.postgresql.PostgresDatabase;
import io.github.dimkich.integration.testing.postgresql.model.metadata.PostgresTableMetadata;
import io.github.dimkich.integration.testing.postgresql.model.metadata.PostgresTypeDescriptor;
import io.github.dimkich.integration.testing.postgresql.serde.PostgresColumnSerdeResolver;
import io.github.dimkich.integration.testing.storage.exclusion.FieldExclusionProcessor;
import io.github.dimkich.integration.testing.storage.sql.table.Table;
import io.github.dimkich.integration.testing.storage.sql.table.TableMap;
import io.github.dimkich.integration.testing.storage.sql.table.TableName;
import io.github.dimkich.integration.testing.storage.sql.table.TableRegistry;
import io.github.dimkich.integration.testing.storage.sql.table.TableSet;
import lombok.RequiredArgsConstructor;

import java.sql.Array;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Repository for PostgreSQL table metadata. Loads columns, primary-key positions and owned
 * sequences of the requested tables in one catalog query and returns ready
 * {@link PostgresTableMetadata} objects.
 */
@RequiredArgsConstructor
public class PostgresTableMetadataRepository {
    private final PostgresDatabase database;
    private final PostgresTypeCatalogRepository typeCatalogRepository;
    private final PostgresProperties.Connection connectionProperties;
    private final PostgresColumnSerdeResolver serdeResolver;
    private final DateTimeService dateTimeService;
    private final FieldExclusionProcessor fieldExclusionProcessor;

    public List<PostgresTableMetadata> load(TableSet tables) throws SQLException {
        if (tables.isEmpty()) {
            return List.of();
        }
        TableRegistry registry = tables.getRegistry();
        TableMap<PostgresTableMetadataBuilder> builders = new TableMap<>(registry.size());

        // Load the type graph before opening the metadata result set: both queries share one JDBC
        // connection, so keeping the catalog reads outside the streaming loop avoids nested cursors.
        typeCatalogRepository.ensureLoaded();

        PostgresRequestedTables requested = PostgresRequestedTables.of(tables);
        String sql = """
                WITH requested AS (
                    SELECT c.oid, n.nspname, c.relname
                    FROM pg_class c
                        JOIN pg_namespace n ON n.oid = c.relnamespace
                    WHERE %s
                      AND (%s)
                ),
                columns AS (
                    SELECT r.oid AS table_oid, a.attnum, a.attname, a.atttypid,
                           a.attgenerated = 's' AS generated,
                           a.attidentity = 'a' AS identity_always
                    FROM requested r
                        JOIN pg_attribute a ON a.attrelid = r.oid
                    WHERE a.attnum > 0
                      AND NOT a.attisdropped
                ),
                primary_keys AS (
                    SELECT i.indrelid AS table_oid, k.attnum, k.ord
                    FROM pg_index i
                        JOIN LATERAL unnest(i.indkey::smallint[]) WITH ORDINALITY AS k(attnum, ord)
                            ON true
                    WHERE i.indisprimary
                      AND i.indrelid IN (SELECT oid FROM requested)
                ),
                distinct_sequences AS (
                    SELECT DISTINCT d.refobjid AS table_oid,
                           format('%%I.%%I', sns.nspname, s.relname) AS sequence_name
                    FROM pg_depend d
                        JOIN pg_class s ON d.objid = s.oid AND s.relkind = 'S'
                        JOIN pg_namespace sns ON s.relnamespace = sns.oid
                    WHERE d.refobjid IN (SELECT oid FROM requested)
                ),
                sequences AS (
                    SELECT table_oid,
                           array_agg(sequence_name ORDER BY sequence_name) AS names
                    FROM distinct_sequences
                    GROUP BY table_oid
                )
                SELECT r.nspname AS schema_name, r.relname AS table_name,
                       c.attnum, c.attname, c.atttypid,
                       c.generated, c.identity_always,
                       pk.ord AS primary_key_ordinal,
                       s.names AS sequences
                FROM requested r
                    JOIN columns c ON c.table_oid = r.oid
                    LEFT JOIN primary_keys pk ON pk.table_oid = r.oid AND pk.attnum = c.attnum
                    LEFT JOIN sequences s ON s.table_oid = r.oid
                ORDER BY r.nspname, r.relname, c.attnum
                """.formatted(PostgresCatalogSql.TABLE_FILTER, requested.predicate());

        return database.extract(sql, statement -> {
            int parameter = PostgresCatalogSql.bindTableFilter(statement, 1);
            requested.bind(statement, parameter);
        }, rs -> {
            String lastSchema = null;
            String lastTableName = null;
            Table lastTable = null;
            while (rs.next()) {
                String schemaName = rs.getString("schema_name");
                String tableName = rs.getString("table_name");
                Table table;
                if (schemaName.equals(lastSchema) && tableName.equals(lastTableName)) {
                    table = lastTable;
                } else {
                    table = registry.find(TableName.of(schemaName, tableName));
                    lastSchema = schemaName;
                    lastTableName = tableName;
                    lastTable = table;
                }
                if (table == null) {
                    continue;
                }
                PostgresTableMetadataBuilder builder = builders.get(table);
                if (builder == null) {
                    builder = newBuilder(table);
                    builders.put(table, builder);
                    Array sequences = rs.getArray("sequences");
                    if (sequences != null) {
                        try {
                            builder.setSequences(Arrays.asList((String[]) sequences.getArray()));
                        } finally {
                            sequences.free();
                        }
                    }
                }
                PostgresTypeDescriptor type = typeCatalogRepository.getType(rs.getInt("atttypid"));
                String columnName = builder.addColumn(rs, type);

                int primaryKeyOrdinal = rs.getInt("primary_key_ordinal");
                if (!rs.wasNull()) {
                    builder.addPrimaryKey(primaryKeyOrdinal, columnName);
                }
            }

            List<PostgresTableMetadata> result = new ArrayList<>(tables.size());
            for (Table table : tables) {
                PostgresTableMetadataBuilder builder = builders.get(table);
                if (builder == null) {
                    builder = newBuilder(table);
                }
                result.add(builder.build(fieldExclusionProcessor));
            }
            return result;
        });
    }

    private PostgresTableMetadataBuilder newBuilder(Table table) {
        return new PostgresTableMetadataBuilder(table, connectionProperties, serdeResolver,
                dateTimeService);
    }
}
