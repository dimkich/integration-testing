package io.github.dimkich.integration.testing.initialization.sql;

import io.github.dimkich.integration.testing.initialization.TestInitState;
import io.github.dimkich.integration.testing.storage.sql.SQLDataStorageService;
import lombok.Getter;
import lombok.ToString;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The effective table actions of the current test: the actions of every storage that is in scope,
 * collected from the inherited and the local {@link OnTableChangeInit} declarations.
 *
 * <p>Merging is done per storage, so a declaration of a storage (including one with an empty action
 * list) replaces the inherited actions of that storage while the other storages are left untouched.
 */
@Getter
@ToString
public class OnTableChangeInitState implements TestInitState<OnTableChangeInitState> {
    private final Map<SQLDataStorageService, List<OnTableChangeInit.TableChangeAction>> actionsByStorage =
            new LinkedHashMap<>();

    void put(SQLDataStorageService storage,
             List<OnTableChangeInit.TableChangeAction> actions) {
        actionsByStorage.put(storage, actions);
    }

    @Override
    public OnTableChangeInitState merge(OnTableChangeInitState state) {
        actionsByStorage.putAll(state.actionsByStorage);
        return this;
    }

    @Override
    public OnTableChangeInitState copy() {
        OnTableChangeInitState copy = new OnTableChangeInitState();
        copy.actionsByStorage.putAll(this.actionsByStorage);
        return copy;
    }
}
