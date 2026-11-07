package io.github.dimkich.integration.testing.postgresql.repository;

import io.github.dimkich.integration.testing.postgresql.PostgresDatabase;
import io.github.dimkich.integration.testing.postgresql.model.metadata.PostgresCompositeField;
import io.github.dimkich.integration.testing.postgresql.model.metadata.PostgresTypeDescriptor;
import io.github.dimkich.integration.testing.postgresql.repository.projection.PostgresCompositeFieldRow;
import io.github.dimkich.integration.testing.postgresql.repository.projection.PostgresTypeRow;
import lombok.RequiredArgsConstructor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loads, resolves and caches the PostgreSQL type graph. The catalog is read once per schema
 * lifetime; {@link #reset()} drops the cache after a schema change.
 */
@RequiredArgsConstructor
public class PostgresTypeCatalogRepository {
    private final PostgresDatabase database;
    private final PostgresCompositeTypeRepository compositeTypeRepository;

    private Map<Integer, PostgresTypeDescriptor> typesCache;

    /**
     * Resolves and returns the descriptor of one PostgreSQL type. The first call loads and resolves
     * the whole {@code pg_type} graph; later calls only read the cache.
     */
    public PostgresTypeDescriptor getType(int oid) throws SQLException {
        ensureLoaded();
        PostgresTypeDescriptor descriptor = typesCache.get(oid);
        if (descriptor == null) {
            throw new IllegalStateException("PostgreSQL type " + oid + " not found");
        }
        return descriptor;
    }

    /**
     * Loads the type graph ahead of a streaming metadata query. This keeps nested catalog queries
     * off an already open {@link ResultSet} while preserving lazy loading for all other callers.
     */
    void ensureLoaded() throws SQLException {
        if (typesCache == null) {
            loadAllTypes();
        }
    }

    /**
     * Drops the cached type graph, so the next access re-reads PostgreSQL catalogs.
     */
    public void reset() {
        typesCache = null;
        // Type resolution depends on composite attributes, so both caches are dropped together.
        compositeTypeRepository.reset();
    }

    private void loadAllTypes() throws SQLException {
        Map<Integer, PostgresTypeRow> rows = loadTypeRows();
        Map<Integer, PostgresTypeDescriptor> descriptors = new HashMap<>();
        for (Integer oid : rows.keySet()) {
            resolveType(oid, rows, descriptors, new HashSet<>());
        }
        typesCache = descriptors;
    }

    private Map<Integer, PostgresTypeRow> loadTypeRows() throws SQLException {
        Map<Integer, PostgresTypeRow> rows = new LinkedHashMap<>();
        String typeSql = """
                SELECT oid, typname, typtype, typelem, typbasetype, typrelid, typdelim
                FROM pg_type
                """;
        for (PostgresTypeRow row : database.query(typeSql, resultSet -> new PostgresTypeRow(
                resultSet.getInt("oid"),
                resultSet.getString("typname"),
                resultSet.getString("typtype").charAt(0),
                resultSet.getInt("typelem"),
                resultSet.getInt("typbasetype"),
                resultSet.getInt("typrelid"),
                resultSet.getString("typdelim").charAt(0)))) {
            rows.put(row.getOid(), row);
        }
        return rows;
    }

    private PostgresTypeDescriptor resolveType(int oid, Map<Integer, PostgresTypeRow> rows,
                                               Map<Integer, PostgresTypeDescriptor> descriptors,
                                               Set<Integer> resolving) throws SQLException {
        PostgresTypeDescriptor cached = descriptors.get(oid);
        if (cached != null) {
            return cached;
        }
        PostgresTypeRow row = rows.get(oid);
        if (row == null) {
            throw new IllegalStateException("PostgreSQL type " + oid + " not found");
        }
        if (!resolving.add(oid)) {
            throw new IllegalStateException("Cyclic PostgreSQL type " + oid);
        }
        PostgresTypeDescriptor result;
        if (row.isDomain()) {
            result = resolveType(row.getBaseOid(), rows, descriptors, resolving);
        } else if (row.isArray()) {
            result = new PostgresTypeDescriptor(row.getName(), 'a',
                    resolveType(row.getElementOid(), rows, descriptors, resolving),
                    List.of(), row.getDelimiter());
        } else if (row.isComposite()) {
            List<PostgresCompositeField> fields = new ArrayList<>();
            for (PostgresCompositeFieldRow field :
                    compositeTypeRepository.getFields(row.getCompositeOid())) {
                fields.add(new PostgresCompositeField(field.getName(),
                        resolveType(field.getTypeOid(), rows, descriptors, resolving)));
            }
            result = new PostgresTypeDescriptor(row.getName(), 'c', null, fields,
                    row.getDelimiter());
        } else {
            result = new PostgresTypeDescriptor(row.getName(), row.getKind(), null, List.of(),
                    row.getDelimiter());
        }
        resolving.remove(oid);
        descriptors.put(oid, result);
        return result;
    }
}
