package io.github.dimkich.integration.testing.postgresql.repository;

import io.github.dimkich.integration.testing.postgresql.PostgresDatabase;
import io.github.dimkich.integration.testing.postgresql.repository.projection.PostgresCompositeFieldRow;
import lombok.RequiredArgsConstructor;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads and caches the attributes of PostgreSQL composite types. The catalog is read once per
 * schema lifetime; {@link #reset()} drops the cache after a schema change.
 */
@RequiredArgsConstructor
public class PostgresCompositeTypeRepository {
    private final PostgresDatabase database;

    private Map<Integer, List<PostgresCompositeFieldRow>> fieldsCache;

    /**
     * Returns the fields of one composite type. The first call loads all composite attributes in a
     * single catalog query; later calls only read the cache.
     */
    public List<PostgresCompositeFieldRow> getFields(int compositeOid) throws SQLException {
        if (fieldsCache == null) {
            loadAllFields();
        }
        return fieldsCache.getOrDefault(compositeOid, List.of());
    }

    /**
     * Drops the cached composite attributes, so the next access re-reads the PostgreSQL catalogs.
     */
    public void reset() {
        fieldsCache = null;
    }

    private void loadAllFields() throws SQLException {
        Map<Integer, List<PostgresCompositeFieldRow>> fields = new HashMap<>();
        String fieldSql = """
                SELECT a.attrelid, a.attname, a.atttypid
                FROM pg_attribute a
                WHERE a.attrelid IN (SELECT typrelid FROM pg_type WHERE typrelid <> 0)
                  AND a.attnum > 0
                  AND NOT a.attisdropped
                ORDER BY a.attrelid, a.attnum
                """;
        for (PostgresCompositeFieldRow field : database.query(fieldSql, resultSet ->
                new PostgresCompositeFieldRow(resultSet.getInt("attrelid"),
                        resultSet.getString("attname"), resultSet.getInt("atttypid")))) {
            fields.computeIfAbsent(field.getCompositeOid(), key -> new ArrayList<>()).add(field);
        }
        fieldsCache = fields;
    }
}
