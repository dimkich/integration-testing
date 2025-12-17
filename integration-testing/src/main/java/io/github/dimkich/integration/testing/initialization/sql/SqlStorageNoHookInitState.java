package io.github.dimkich.integration.testing.initialization.sql;

import io.github.dimkich.integration.testing.initialization.TestInitState;
import io.github.dimkich.integration.testing.storage.sql.SQLDataStorageService;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

@ToString
@NoArgsConstructor
public class SqlStorageNoHookInitState implements TestInitState<SqlStorageNoHookInitState> {
    private final Map<SQLDataStorageService, List<String>> map = new LinkedHashMap<>();

    public SqlStorageNoHookInitState(SQLDataStorageService service, List<String> sql) {
        map.put(service, sql);
    }

    @Override
    public SqlStorageNoHookInitState merge(SqlStorageNoHookInitState state) {
        for (Map.Entry<SQLDataStorageService, List<String>> entry : state.map.entrySet()) {
            map.compute(entry.getKey(), (k, v) -> {
                if (v == null) {
                    return entry.getValue();
                }
                v.addAll(entry.getValue());
                return v;
            });
        }
        return this;
    }

    @Override
    public SqlStorageNoHookInitState copy() {
        SqlStorageNoHookInitState state = new SqlStorageNoHookInitState();
        state.map.putAll(map);
        return state;
    }

    public void foreach(BiConsumer<SQLDataStorageService, List<String>> consumer) {
        map.forEach(consumer);
    }
}
