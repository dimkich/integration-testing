package io.github.dimkich.integration.testing.storage.sql.state;

import io.github.dimkich.integration.testing.storage.sql.table.Table;
import io.github.dimkich.integration.testing.storage.sql.table.TableMap;
import io.github.dimkich.integration.testing.storage.sql.table.TableRegistry;

/**
 * Baseline statuses of the tables. A missing value means that the state does not declare the
 * table; {@code LOADED} and {@code CLEARED} are explicit declarations.
 */
public final class TableBaseline {
    private enum Status {
        LOADED, CLEARED
    }

    private final TableRegistry registry;
    private TableMap<Status> statuses;

    public TableBaseline(TableRegistry registry) {
        this.registry = registry;
        this.statuses = new TableMap<>(registry.size());
    }

    public boolean isLoaded(Table table) {
        return statuses.get(table) == Status.LOADED;
    }

    public boolean isCleared(Table table) {
        return statuses.get(table) == Status.CLEARED;
    }

    /**
     * Returns {@code true} when the table is declared in this baseline and its status differs from
     * the other baseline. Undeclared tables never force a reset.
     */
    public boolean differsFrom(TableBaseline other, Table table) {
        Status status = statuses.get(table);
        return status != null && status != other.statuses.get(table);
    }

    public void setLoaded(Table table) {
        statuses.put(table, Status.LOADED);
    }

    public void setCleared(Table table) {
        statuses.put(table, Status.CLEARED);
    }

    /**
     * Copies all declared statuses from {@code other}. Undeclared tables do not override the
     * current state.
     */
    public void merge(TableBaseline other) {
        for (int id = 0; id < other.registry.size(); id++) {
            Table table = other.registry.get(id);
            Status status = other.statuses.get(table);
            if (status != null) {
                statuses.put(table, status);
            }
        }
    }

    public void copyFrom(TableBaseline other) {
        statuses = other.statuses.copy();
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("{");
        boolean first = true;
        for (int id = 0; id < registry.size(); id++) {
            Table table = registry.get(id);
            Status status = statuses.get(table);
            if (status == null) {
                continue;
            }
            if (!first) {
                result.append(", ");
            }
            result.append(table.getName()).append('=').append(status);
            first = false;
        }
        return result.append('}').toString();
    }
}
