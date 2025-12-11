package io.github.dimkich.integration.testing.initialization.sql;

import io.github.dimkich.integration.testing.initialization.TestInitState;
import io.github.dimkich.integration.testing.storage.sql.SQLDataStorageService;
import io.github.dimkich.integration.testing.storage.sql.state.TableStates;
import lombok.NoArgsConstructor;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@NoArgsConstructor
public class SqlStorageInitState implements TestInitState<SqlStorageInitState> {
    private SQLDataStorageService storage;
    private TableStates tableStates;
    private Map<SQLDataStorageService, TableStates> map;

    public SqlStorageInitState(SQLDataStorageService storage, TableStates tableStates) {
        this.storage = storage;
        this.tableStates = tableStates;
    }

    public TableStates getTableStates(SQLDataStorageService storage) {
        if (this.storage != null) {
            return this.storage == storage ? this.tableStates : null;
        }
        return map.get(storage);
    }

    public Set<Map.Entry<SQLDataStorageService, TableStates>> getEntries() {
        if (this.storage != null) {
            return Set.of(new AbstractMap.SimpleEntry<>(this.storage, tableStates));
        }
        return map == null ? Set.of() : map.entrySet();
    }

    @Override
    public SqlStorageInitState merge(SqlStorageInitState state) {
        if (storage != null && storage == state.storage) {
            this.tableStates.merge(state.tableStates);
            return this;
        }
        if (map == null) {
            map = new HashMap<>();
            map.put(storage, tableStates);
            storage = null;
            tableStates = null;
        }
        if (state.storage != null) {
            map.compute(state.storage, (storage, states) -> compute(states, state.tableStates));
            return this;
        }
        for (Map.Entry<SQLDataStorageService, TableStates> entry : state.map.entrySet()) {
            map.compute(entry.getKey(), (storage, states) -> compute(states, entry.getValue()));
        }
        return this;
    }

    private TableStates compute(TableStates oldStates, TableStates newStates) {
        if (oldStates == null) {
            return newStates;
        }
        oldStates.merge(newStates);
        return oldStates;
    }

    @Override
    public SqlStorageInitState copy() {
        SqlStorageInitState state = new SqlStorageInitState();
        state.storage = storage;
        state.tableStates = tableStates == null ? null : tableStates.copy();
        if (map != null) {
            state.map = new HashMap<>();
            for (Map.Entry<SQLDataStorageService, TableStates> entry : map.entrySet()) {
                state.map.put(entry.getKey(), entry.getValue().copy());
            }
        }
        return state;
    }
}
