package io.github.dimkich.integration.testing.storage.sql.table;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Maps table names to canonical {@link Table}s. The mapping is append-only: existing table ids
 * never change. A {@code Table} is the internal identity and metadata owner; configuration and SQL
 * exchange {@link TableName} values at their boundaries.
 * <p>
 * Unqualified names are always resolved in the configured default schema. Names that already have
 * a schema are used as is.
 */
@RequiredArgsConstructor
public class TableRegistry {
    @NonNull
    private final String defaultSchema;

    private Table[] byId = new Table[16];
    private int size;
    private final Map<TableName, Table> byName = new HashMap<>();
    private final BitSet currentTables = new BitSet();

    /**
     * Registers a table name during initialization. Re-registering the same name is idempotent.
     */
    public Table register(TableName name) {
        TableName registeredName = name.qualify(defaultSchema);
        Table existing = byName.get(registeredName);
        if (existing != null) {
            return existing;
        }
        int newIndex = size++;
        if (newIndex == byId.length) {
            byId = Arrays.copyOf(byId, byId.length * 2);
        }
        String displayName = registeredName.getSchema().equals(defaultSchema)
                ? registeredName.getUnqualified()
                : registeredName.getCanonicalName();
        Table id = new Table(newIndex, registeredName, displayName);
        byId[newIndex] = id;
        byName.put(registeredName, id);
        return id;
    }

    public Table register(String name) {
        return register(TableName.parse(name));
    }

    /**
     * Resolves a configured or audit name. It never creates a table: an unknown name is a
     * configuration error and must be reported immediately.
     */
    public Table resolve(String name) {
        return resolve(TableName.parse(name));
    }

    public Table resolve(TableName name) {
        Table resolved = find(name);
        if (resolved == null) {
            throw new IllegalArgumentException("Table '" + name + "' is not registered in the storage");
        }
        return resolved;
    }

    /**
     * Returns the registered table or {@code null}. The name is resolved exactly after applying the
     * default schema to an unqualified name.
     */
    public Table find(TableName name) {
        return byName.get(name.qualify(defaultSchema));
    }

    public Table resolveOrRegister(TableName name) {
        Table table = find(name);
        return table != null ? table : register(name);
    }

    /**
     * Replaces the current table set. Existing ids are preserved.
     */
    public void updateTables(Collection<TableName> tableNames) {
        BitSet updated = new BitSet();
        for (TableName table : tableNames) {
            updated.set(register(table).getId());
        }
        currentTables.clear();
        currentTables.or(updated);
    }

    /**
     * Ids of the tables currently present in the storage. The returned set is a live view owned by
     * the registry and must not be mutated by callers.
     */
    public TableSet getCurrentTables() {
        return tableSet(currentTables);
    }

    /**
     * All registered table ids, including tables that are no longer present in the storage.
     */
    public TableSet getAllTables() {
        BitSet all = new BitSet(size);
        all.set(0, size);
        return tableSet(all);
    }

    TableSet tableSet(BitSet bits) {
        return new TableSet(this, bits);
    }

    public Table get(int index) {
        Objects.checkIndex(index, size);
        return byId[index];
    }

    /**
     * Number of all registered names, including tables that are no longer present in the storage.
     */
    public int size() {
        return size;
    }
}
