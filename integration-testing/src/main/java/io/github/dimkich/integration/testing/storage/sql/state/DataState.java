package io.github.dimkich.integration.testing.storage.sql.state;

import io.github.dimkich.integration.testing.initialization.sql.SqlStorageSetup;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the state of data for a table, including SQL statements and state transitions.
 * This class manages the merging and comparison of data states to optimize SQL execution
 * by detecting overlapping sequences and avoiding redundant operations.
 */
@Data
public class DataState {
    /**
     * Represents the current state of the table data.
     */
    public enum State {LOADED, CLEARED}

    private State state;

    private List<String> sqls = new ArrayList<>();
    private List<String> noHookSqls = new ArrayList<>();
    private SqlStorageSetup.TableHook tableHook;

    private boolean dirty;

    /**
     * Merges another DataState into this one, combining SQL statements and updating the state.
     * The merge operation detects overlapping SQL sequences to avoid duplication.
     *
     * @param state the DataState to merge into this one (must not be null)
     */
    public void merge(DataState state) {
        if (shouldClearSqls(state)) {
            if (hasStateChanged(state)) {
                this.state = state.state;
            }
            this.sqls.clear();
            this.noHookSqls.clear();
        }

        if (!state.sqls.isEmpty()) {
            this.sqls.addAll(this.noHookSqls);
            this.noHookSqls.clear();
        }

        mergeSqlLists(this.sqls, state.sqls);
        mergeSqlLists(this.noHookSqls, state.noHookSqls);
        this.dirty = false;
    }

    /**
     * Compares this DataState with another and populates a visitor with the differences.
     * The visitor is used to determine what actions need to be taken to synchronize states.
     *
     * @param state     the DataState to compare with (must not be null)
     * @param tableName the name of the table being compared
     * @param visitor   the visitor to populate with differences (must not be null)
     */
    public void compare(DataState state, String tableName, TablesActionVisitor visitor) {
        if (shouldTriggerStateChange(state, visitor)) {
            if (tableHook != null) {
                visitor.getHooks().add(tableHook);
            }
            visitor.getTablesToRestartIdentity().add(tableName);
            State targetState = state.state != null ? state.state : this.state;
            switch (targetState) {
                case LOADED:
                    visitor.getTablesToLoad().add(tableName);
                    break;
                case CLEARED:
                    visitor.getTablesToClear().add(tableName);
                    break;
            }
        }

        int sqlStartIndex = findOverlappingSuffixLength(this.sqls, state.sqls);
        for (int i = sqlStartIndex; i < state.sqls.size(); i++) {
            if (tableHook != null) {
                visitor.getHooks().add(tableHook);
            }
            visitor.getSqls().add(state.sqls.get(i));
        }

        int noHookSqlStartIndex = findOverlappingSuffixLength(this.noHookSqls, state.noHookSqls);
        for (int i = noHookSqlStartIndex; i < state.noHookSqls.size(); i++) {
            visitor.getNoHookSqls().add(state.noHookSqls.get(i));
        }
    }

    /**
     * Creates a deep copy of this DataState.
     *
     * @return a new DataState instance with copied values
     */
    public DataState copy() {
        DataState state = new DataState();
        state.state = this.state;
        state.sqls = new ArrayList<>(sqls);
        state.noHookSqls = new ArrayList<>(noHookSqls);
        state.tableHook = tableHook;
        state.dirty = dirty;
        return state;
    }

    /**
     * Determines if SQL lists should be cleared based on state changes or dirty flag.
     *
     * @param other the other DataState to compare with
     * @return true if SQL lists should be cleared
     */
    private boolean shouldClearSqls(DataState other) {
        return hasStateChanged(other) || dirty;
    }

    /**
     * Checks if the state has changed between this and another DataState.
     *
     * @param other the other DataState to compare with
     * @return true if the state has changed
     */
    private boolean hasStateChanged(DataState other) {
        return other.state != null && this.state != other.state;
    }

    /**
     * Determines if a state change should trigger actions in the visitor.
     *
     * @param other   the other DataState to compare with
     * @param visitor the visitor to check dirty flag with
     * @return true if state change actions should be triggered
     */
    private boolean shouldTriggerStateChange(DataState other, TablesActionVisitor visitor) {
        return hasStateChanged(other) || (dirty && visitor.isCheckDirty());
    }

    /**
     * Merges SQL lists by detecting overlapping suffixes and only adding new elements.
     *
     * @param target the target list to merge into
     * @param source the source list to merge from
     */
    private void mergeSqlLists(List<String> target, List<String> source) {
        int startIndex = findOverlappingSuffixLength(target, source);
        for (int i = startIndex; i < source.size(); i++) {
            target.add(source.get(i));
        }
    }

    /**
     * Finds the length of the overlapping suffix between two lists.
     * This method checks if the tail of the first list matches the head of the second list,
     * and returns the index in the second list where new elements should start being added.
     *
     * @param tail the list whose tail (end) is checked
     * @param head the list whose head (beginning) is checked
     * @return the index in the head list where new elements should start (0 if no overlap)
     */
    private int findOverlappingSuffixLength(List<String> tail, List<String> head) {
        if (tail.isEmpty() || head.isEmpty()) {
            return 0;
        }

        int maxOverlap = Math.min(tail.size(), head.size());
        for (int overlapLength = maxOverlap; overlapLength > 0; overlapLength--) {
            boolean matches = true;
            for (int i = 0; i < overlapLength; i++) {
                int tailIndex = tail.size() - overlapLength + i;
                if (!tail.get(tailIndex).equals(head.get(i))) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return overlapLength;
            }
        }
        return 0;
    }
}
