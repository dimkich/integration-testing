package io.github.dimkich.integration.testing.storage.sql.state;

import io.github.dimkich.integration.testing.initialization.sql.SqlStorageSetup;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

/**
 * Visitor class that collects actions to be performed on database tables based on state comparisons.
 * <p>
 * This class implements the visitor pattern to accumulate differences between table states.
 * It collects various actions such as table access changes, data state changes, SQL statements,
 * and hooks that need to be executed to synchronize the database state.
 * </p>
 * <p>
 * The visitor is populated by calling {@link TableState#compare(TableState, String, TablesActionVisitor)}
 * for each table, and then the collected actions can be executed to apply the necessary changes.
 * </p>
 */
@Getter
public class TablesActionVisitor {
    /**
     * Flag indicating whether dirty state checking should be enabled.
     * When true, dirty tables will trigger state change actions even if the state itself hasn't changed.
     */
    @Setter
    private boolean checkDirty;

    /**
     * Tables that need to be allowed (access changed from RESTRICTED to ALLOWED).
     * Uses LinkedHashSet to preserve insertion order, which may be important for execution.
     */
    private final Set<String> tablesToAllow = new LinkedHashSet<>();

    /**
     * Tables that need to be denied/restricted (access changed from ALLOWED to RESTRICTED).
     * Uses LinkedHashSet to preserve insertion order, which may be important for execution.
     */
    private final Set<String> tablesToDeny = new LinkedHashSet<>();

    /**
     * Tables that need to be cleared (state changed to CLEARED).
     * Uses HashSet since order doesn't matter for clearing operations.
     */
    private final Set<String> tablesToClear = new HashSet<>();

    /**
     * Tables that need identity restart before loading data.
     * Uses HashSet since order doesn't matter for restart operations.
     */
    private final Set<String> tablesToRestartIdentity = new HashSet<>();

    /**
     * Tables that need to be loaded with data (state changed to LOADED).
     * Uses HashSet since order doesn't matter for loading operations.
     */
    private final Set<String> tablesToLoad = new HashSet<>();

    /**
     * SQL statements to be executed, in execution order.
     * Uses ArrayDeque to support efficient prepending with addFirst() for prioritizing certain operations.
     */
    private final Deque<String> sqls = new ArrayDeque<>();

    /**
     * SQL statements to be executed without table hooks.
     * Uses LinkedHashSet to preserve insertion order and avoid duplicates.
     */
    private final Set<String> noHookSqls = new LinkedHashSet<>();

    /**
     * Table hooks to be executed after SQL operations.
     * Uses LinkedHashSet to preserve insertion order and avoid duplicate hook executions.
     */
    private final Set<SqlStorageSetup.TableHook> hooks = new LinkedHashSet<>();

    /**
     * Checks if this visitor has collected any actions to be performed.
     *
     * @return true if any collections contain elements, false otherwise
     */
    public boolean isAnyChanges() {
        return !tablesToAllow.isEmpty()
                || !tablesToDeny.isEmpty()
                || !tablesToClear.isEmpty()
                || !tablesToRestartIdentity.isEmpty()
                || !tablesToLoad.isEmpty()
                || !sqls.isEmpty()
                || !noHookSqls.isEmpty()
                || !hooks.isEmpty();
    }

    /**
     * Clears all collected actions, resetting the visitor to an empty state.
     * This method is typically called after applying the collected actions.
     */
    public void clear() {
        tablesToAllow.clear();
        tablesToDeny.clear();
        tablesToClear.clear();
        tablesToRestartIdentity.clear();
        tablesToLoad.clear();
        sqls.clear();
        noHookSqls.clear();
        hooks.clear();
    }
}
