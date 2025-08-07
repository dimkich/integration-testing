package io.github.dimkich.integration.testing.storage.sql.state;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableState {
    public enum Access {
        ALLOWED, RESTRICTED;

        public void diff(Access access, String tableName, TablesActionVisitor visitor) {
            if (this == access || access == null) {
                return;
            }
            switch (access) {
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

    public TableState diff(TableState state, String tableName, TablesActionVisitor visitor) {
        if (access != null) {
            access.diff(state.access, tableName, visitor);
        }
        if (state.access != null) {
            access = state.access;
        }
        data.merge(state.data, tableName, visitor);
        return this;
    }
}
