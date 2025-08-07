package io.github.dimkich.integration.testing.storage.sql.state;

import io.github.dimkich.integration.testing.initialization.SqlStorageInit;
import io.github.dimkich.integration.testing.initialization.SqlStorageSetup;
import net.sf.jsqlparser.parser.CCJSqlParser;
import net.sf.jsqlparser.parser.ParseException;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static io.github.dimkich.integration.testing.storage.sql.state.DataState.State.CLEARED;
import static io.github.dimkich.integration.testing.storage.sql.state.DataState.State.LOADED;
import static io.github.dimkich.integration.testing.storage.sql.state.TableState.Access.ALLOWED;
import static io.github.dimkich.integration.testing.storage.sql.state.TableState.Access.RESTRICTED;

public class TableStates {
    private final Map<String, TableState> tableStates = new HashMap<>();

    public static TableStates createDefault(Collection<String> tables,
                                            Map<String, SqlStorageSetup.TableHook> tableHooks) {
        TableStates tableStates = new TableStates();
        for (String name : tables) {
            TableState state = new TableState();
            state.setAccess(RESTRICTED);
            state.getData().setState(CLEARED);
            state.getData().setDirty(true);
            state.getData().setTableHook(tableHooks.get(name));
            tableStates.tableStates.put(name, state);
        }
        return tableStates;
    }

    public static TableStates createFromInit(Collection<String> tables, SqlStorageInit init) throws ParseException {
        boolean loadAllTables = init.getLoadAllTables() != null && init.getLoadAllTables();
        boolean disableTableHooks = init.getDisableTableHooks() != null && init.getDisableTableHooks();
        if (disableTableHooks && (init.getTablesToChange() != null || init.getTablesToLoad() != null || loadAllTables)) {
            throw new IllegalArgumentException("disableTableHooks supported only for sqls");
        }

        TableStates tableStates = new TableStates();
        for (String name : stringToSet(init.getTablesToChange())) {
            TableState state = tableStates.tableStates.computeIfAbsent(name, n -> new TableState());
            state.setAccess(ALLOWED);
            state.getData().setState(CLEARED);
        }

        for (String name : loadAllTables ? tables : stringToSet(init.getTablesToLoad())) {
            TableState state = tableStates.tableStates.computeIfAbsent(name, n -> new TableState());
            state.getData().setState(LOADED);
        }

        if (init.getSql() != null) {
            for (String sql : init.getSql()) {
                for (String name : getTableList(sql)) {
                    TableState state = tableStates.tableStates.computeIfAbsent(name, n -> new TableState());
                    if (disableTableHooks) {
                        state.getData().getNoHookSqls().add(sql);
                    } else {
                        state.getData().getSqls().add(sql);
                    }
                }
            }
        }
        return tableStates;
    }

    public void merge(TableStates tableStates) {
        merge(tableStates, null);
    }

    public void merge(TableStates tableStates, TablesActionVisitor visitor) {
        for (Map.Entry<String, TableState> entry : tableStates.tableStates.entrySet()) {
            this.tableStates.compute(entry.getKey(),
                    (table, state) -> {
                        if (state == null) {
                            state = new TableState();
                        }
                        return state.diff(entry.getValue(), table, visitor);
                    });
        }
    }

    public void setDirtyTables(Collection<String> tables) {
        for (String tableName : tables) {
            TableState state = tableStates.get(tableName);
            if (state != null) {
                state.getData().setDirty(true);
            }
        }
    }

    public void clear() {
        tableStates.clear();
    }

    private static Set<String> stringToSet(String string) {
        if (string == null || string.isEmpty()) {
            return Set.of();
        }
        return Arrays.stream(string.split(","))
                .map(String::strip)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static List<String> getTableList(String sql) throws ParseException {
        CCJSqlParser parser = new CCJSqlParser(sql);
        Statements statements = parser.me().Statements();
        TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
        List<String> tables = new ArrayList<>();
        for (Statement statement : statements.getStatements()) {
            tables.addAll(tablesNamesFinder.getTableList(statement));
        }
        return tables;
    }
}
