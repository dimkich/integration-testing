package io.github.dimkich.integration.testing.storage.sql.state;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the state of a database table, including access permissions and data state.
 * This class manages merging and comparing table states to determine what actions
 * need to be taken to synchronize database tables.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableState {
    /**
     * Represents the access level for a table.
     */
    public enum Access {
        ALLOWED, RESTRICTED;

        /**
         * Compares this access level with another and populates the visitor with
         * the necessary actions to transition from this access level to the target access level.
         *
         * @param targetAccess the target access level to transition to
         * @param tableName    the name of the table being compared
         * @param visitor      the visitor to populate with actions (must not be null)
         */
        public void compare(Access targetAccess, String tableName, TablesActionVisitor visitor) {
            // No action needed if access levels are the same or target is null
            if (this == targetAccess || targetAccess == null) {
                return;
            }

            // Determine the action needed based on target access level
            switch (targetAccess) {
                case ALLOWED:
                    visitor.getTablesToAllow().add(tableName);
                    break;
                case RESTRICTED:
                    visitor.getTablesToDeny().add(tableName);
                    break;
            }
        }
    }

    private Access access;
    private DataState data = new DataState();

    /**
     * Merges another TableState into this one, updating access and data state.
     * The merge operation combines states, with the other state's values taking precedence
     * when they are non-null.
     *
     * @param state the TableState to merge into this one (must not be null)
     * @return this instance for method chaining
     */
    public TableState merge(TableState state) {
        if (state.access != null) {
            access = state.access;
        }
        data.merge(state.data);
        return this;
    }

    /**
     * Compares this TableState with another and populates a visitor with the differences.
     * The visitor is used to determine what actions need to be taken to synchronize states.
     *
     * @param state     the TableState to compare with (must not be null)
     * @param tableName the name of the table being compared (must not be null)
     * @param visitor   the visitor to populate with differences (must not be null)
     * @return this instance for method chaining
     */
    public TableState compare(TableState state, String tableName, TablesActionVisitor visitor) {
        if (access != null) {
            access.compare(state.access, tableName, visitor);
        }
        data.compare(state.data, tableName, visitor);
        return this;
    }

    /**
     * Creates a deep copy of this TableState.
     *
     * @return a new TableState instance with copied values
     */
    public TableState copy() {
        TableState state = new TableState();
        state.access = this.access;
        state.data = data.copy();
        return state;
    }
}
