package io.github.dimkich.integration.testing.initialization.sql;

import io.github.dimkich.integration.testing.initialization.TestInitState;
import io.github.dimkich.integration.testing.storage.sql.SQLDataStorageService;
import io.github.dimkich.integration.testing.storage.sql.state.StorageState;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@ToString
@NoArgsConstructor
public class SqlStorageInitState implements TestInitState<SqlStorageInitState> {
    private SQLDataStorageService storage;
    private StorageState storageState;
    private Map<SQLDataStorageService, StorageState> statesByStorage;

    public SqlStorageInitState(SQLDataStorageService storage, StorageState storageState) {
        this.storage = storage;
        this.storageState = storageState;
    }

    public StorageState getStorageState(SQLDataStorageService storage) {
        if (this.storage != null) {
            return this.storage == storage ? this.storageState : null;
        }
        return statesByStorage == null ? null : statesByStorage.get(storage);
    }

    public Set<Map.Entry<SQLDataStorageService, StorageState>> getEntries() {
        if (this.storage != null) {
            return Set.of(new AbstractMap.SimpleEntry<>(this.storage, storageState));
        }
        return statesByStorage == null ? Set.of() : statesByStorage.entrySet();
    }

    @Override
    public SqlStorageInitState merge(SqlStorageInitState state) {
        if (storage != null && storage == state.storage) {
            this.storageState.merge(state.storageState);
            return this;
        }
        if (statesByStorage == null) {
            statesByStorage = new LinkedHashMap<>();
            if (storage != null) {
                statesByStorage.put(storage, storageState);
            }
            storage = null;
            storageState = null;
        }
        if (state.storage != null) {
            statesByStorage.compute(state.storage, (storage, states) -> compute(states, state.storageState));
            return this;
        }
        if (state.statesByStorage != null) {
            for (Map.Entry<SQLDataStorageService, StorageState> entry : state.statesByStorage.entrySet()) {
                statesByStorage.compute(entry.getKey(), (storage, states) -> compute(states, entry.getValue()));
            }
        }
        return this;
    }

    private StorageState compute(StorageState oldState, StorageState newState) {
        if (oldState == null) {
            return newState;
        }
        oldState.merge(newState);
        return oldState;
    }

    @Override
    public SqlStorageInitState copy() {
        SqlStorageInitState state = new SqlStorageInitState();
        state.storage = storage;
        state.storageState = storageState == null ? null : storageState.copy();
        if (statesByStorage != null) {
            state.statesByStorage = new LinkedHashMap<>();
            for (Map.Entry<SQLDataStorageService, StorageState> entry : statesByStorage.entrySet()) {
                state.statesByStorage.put(entry.getKey(), entry.getValue().copy());
            }
        }
        return state;
    }
}
