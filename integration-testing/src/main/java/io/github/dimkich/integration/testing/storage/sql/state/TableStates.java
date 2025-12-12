package io.github.dimkich.integration.testing.storage.sql.state;

import io.github.dimkich.integration.testing.initialization.sql.SqlStorageInit;
import io.github.dimkich.integration.testing.initialization.sql.SqlStorageSetup;
import io.github.dimkich.integration.testing.storage.sql.SQLDataStorageService;
import net.sf.jsqlparser.parser.CCJSqlParser;
import net.sf.jsqlparser.parser.ParseException;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.dbunit.dataset.DataSetException;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static io.github.dimkich.integration.testing.storage.sql.state.DataState.State.CLEARED;
import static io.github.dimkich.integration.testing.storage.sql.state.DataState.State.LOADED;
import static io.github.dimkich.integration.testing.storage.sql.state.TableState.Access.ALLOWED;
import static io.github.dimkich.integration.testing.storage.sql.state.TableState.Access.RESTRICTED;

/**
 * Manages the state of multiple database tables for integration testing.
 * This class maintains a collection of table states, each representing the access permissions
 * and data state (loaded/cleared) of a database table. It provides functionality to create,
 * merge, compare, and manage table states across test initialization and execution.
 * <p>
 * The class supports operations such as:
 * <ul>
 *   <li>Creating default states for a set of tables</li>
 *   <li>Creating states from initialization configuration</li>
 *   <li>Merging states from different sources</li>
 *   <li>Comparing states to determine required actions</li>
 *   <li>Tracking dirty tables that need to be reloaded</li>
 * </ul>
 */
public class TableStates {
    /**
     * Map of table names to their corresponding table states.
     */
    private final Map<String, TableState> tableStates = new HashMap<>();

    /**
     * Creates a default TableStates instance for the specified tables.
     * Each table is initialized with restricted access and a cleared data state.
     * Table hooks are associated with their corresponding tables if provided.
     *
     * @param tables     the collection of table names to initialize (must not be null)
     * @param tableHooks a map of table names to their corresponding table hooks (can be null or empty)
     * @return a new TableStates instance with default states for all specified tables
     */
    public static TableStates createDefault(Collection<String> tables,
                                            Map<String, List<SqlStorageSetup.TableHook>> tableHooks) {
        TableStates tableStates = new TableStates();
        for (String name : tables) {
            TableState state = new TableState();
            state.setAccess(RESTRICTED);
            state.getData().setState(CLEARED);
            state.getData().setTableHooks(tableHooks.get(name));
            tableStates.tableStates.put(name, state);
        }
        return tableStates;
    }

    /**
     * Creates a TableStates instance from initialization configuration.
     * <p>
     * This method processes the initialization configuration to determine:
     * <ul>
     *   <li>Tables that should have access allowed (tables to change)</li>
     *   <li>Tables that should be loaded with data (tables to load or all loadable tables)</li>
     *   <li>SQL statements to execute and which tables they affect</li>
     * </ul>
     * <p>
     * If {@code disableTableHooks} is enabled, SQL statements are added to the no-hook list,
     * and this option can only be used when no other table configurations are specified.
     *
     * @param storage the SQL data storage service providing table information (must not be null)
     * @param init    the initialization configuration (must not be null)
     * @return a new TableStates instance configured according to the initialization settings
     * @throws ParseException           if SQL parsing fails when extracting table names from SQL statements
     * @throws DataSetException         if there's an error accessing the data set
     * @throws IllegalArgumentException if {@code disableTableHooks} is enabled with other table configurations
     */
    public static TableStates createFromInit(SQLDataStorageService storage, SqlStorageInit init) throws ParseException, DataSetException {
        boolean loadAllTables = init.getLoadAllTables() != null && init.getLoadAllTables();
        boolean disableTableHooks = init.getDisableTableHooks() != null && init.getDisableTableHooks();
        if (disableTableHooks && (init.getTablesToChange() != null || init.getTablesToLoad() != null || loadAllTables)) {
            throw new IllegalArgumentException("disableTableHooks supported only for sqls");
        }

        TableStates tableStates = new TableStates();
        for (String name : parseCommaSeparatedNames(init.getTablesToChange())) {
            TableState state = tableStates.tableStates.computeIfAbsent(name, n -> new TableState());
            state.setAccess(ALLOWED);
            state.getData().setState(CLEARED);
        }

        for (String name : loadAllTables ? storage.getLoadableTables() : parseCommaSeparatedNames(init.getTablesToLoad())) {
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

    /**
     * Merges another TableStates instance into this one.
     * <p>
     * For each table in the source TableStates, the corresponding state in this instance
     * is merged. If a table doesn't exist in this instance, a new TableState is created
     * before merging. The merge operation combines access permissions and data states,
     * with the source state taking precedence.
     *
     * @param tableStates the TableStates instance to merge into this one (must not be null)
     */
    public void merge(TableStates tableStates) {
        for (Map.Entry<String, TableState> entry : tableStates.tableStates.entrySet()) {
            this.tableStates.compute(entry.getKey(),
                    (table, state) -> {
                        if (state == null) {
                            state = new TableState();
                        }
                        return state.merge(entry.getValue());
                    });
        }
    }

    /**
     * Compares this TableStates instance with another and populates a visitor with the differences.
     * <p>
     * This method identifies what actions need to be taken to synchronize the current state
     * with the target state. For each table in the target state, the comparison determines:
     * <ul>
     *   <li>Access permission changes (allow/restrict)</li>
     *   <li>Data state changes (load/clear)</li>
     *   <li>SQL statements that need to be executed</li>
     * </ul>
     * If a table doesn't exist in this instance, a new TableState is created for comparison.
     *
     * @param tableStates the target TableStates instance to compare with (must not be null)
     * @param visitor     the visitor to populate with required actions (must not be null)
     */
    public void compare(TableStates tableStates, TablesActionVisitor visitor) {
        for (Map.Entry<String, TableState> entry : tableStates.tableStates.entrySet()) {
            this.tableStates.compute(entry.getKey(),
                    (table, state) -> {
                        if (state == null) {
                            state = new TableState();
                        }
                        return state.compare(entry.getValue(), table, visitor);
                    });
        }
    }

    /**
     * Marks the specified tables as dirty, indicating they need to be reloaded.
     * Only tables that exist in this TableStates instance are marked as dirty.
     * Tables that don't exist are silently ignored.
     *
     * @param tables the collection of table names to mark as dirty (must not be null)
     */
    public void setDirtyTables(Collection<String> tables) {
        for (String tableName : tables) {
            TableState state = tableStates.get(tableName);
            if (state != null) {
                state.getData().setDirty(true);
            }
        }
    }

    /**
     * Clears all table states from this instance.
     * After calling this method, the instance will be empty.
     */
    public void clear() {
        tableStates.clear();
    }

    /**
     * Creates a deep copy of this TableStates instance.
     * All table states are copied, so modifications to the copy will not affect the original.
     *
     * @return a new TableStates instance that is a deep copy of this one
     */
    public TableStates copy() {
        TableStates states = new TableStates();
        for (Map.Entry<String, TableState> entry : tableStates.entrySet()) {
            states.tableStates.put(entry.getKey(), entry.getValue().copy());
        }
        return states;
    }

    /**
     * Converts a comma-separated string of table names into a set.
     * Empty strings and null values result in an empty set.
     * Whitespace is trimmed from each table name, and empty entries are filtered out.
     *
     * @param string the comma-separated string of table names (can be null or empty)
     * @return a LinkedHashSet containing the parsed table names, maintaining insertion order
     */
    private static Set<String> parseCommaSeparatedNames(String string) {
        if (string == null || string.isEmpty()) {
            return Set.of();
        }
        return Arrays.stream(string.split(","))
                .map(String::strip)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Extracts table names from a SQL statement or block of SQL statements.
     * <p>
     * This method parses the SQL and identifies all tables referenced in the statements.
     * Multiple statements separated by semicolons are supported, and all referenced tables
     * from all statements are collected.
     *
     * @param sql the SQL statement or block to parse (must not be null)
     * @return a list of table names referenced in the SQL statements
     * @throws ParseException if the SQL cannot be parsed
     */
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
