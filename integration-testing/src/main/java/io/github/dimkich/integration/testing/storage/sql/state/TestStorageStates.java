package io.github.dimkich.integration.testing.storage.sql.state;

import eu.ciechanowiec.sneakyfun.SneakyFunction;
import io.github.dimkich.integration.testing.initialization.SqlStorageInit;
import io.github.dimkich.integration.testing.storage.sql.SQLDataStorageService;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class TestStorageStates {
    private TableStates currentStates;
    private final TableStates newStates = new TableStates();
    private final Map<SqlStorageInit, TableStates> statesCache = new LinkedHashMap<>();

    public void init(SQLDataStorageService storage) {
        if (currentStates == null) {
            currentStates = TableStates.createDefault(storage.getTables(), storage.getTableHooks());
        } else {
            currentStates.init(storage.getTableHooks());
        }
        newStates.clear();
        statesCache.clear();
    }

    public void add(SQLDataStorageService storage, SqlStorageInit init) throws Exception {
        TableStates transition = statesCache.computeIfAbsent(init,
                SneakyFunction.sneaky(i -> TableStates.createFromInit(storage.getTables(), i)));
        newStates.merge(transition);
    }

    public void apply(TablesActionVisitor visitor) {
        currentStates.merge(newStates, visitor);
        newStates.clear();
    }

    public void setDirtyTables(Collection<String> tableNames) {
        currentStates.setDirtyTables(tableNames);
    }
}
