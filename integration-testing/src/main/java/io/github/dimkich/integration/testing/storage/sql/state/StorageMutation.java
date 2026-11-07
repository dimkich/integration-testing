package io.github.dimkich.integration.testing.storage.sql.state;

import io.github.dimkich.integration.testing.storage.sql.table.Table;
import io.github.dimkich.integration.testing.storage.sql.table.TableMap;
import io.github.dimkich.integration.testing.storage.sql.table.TableRegistry;
import io.github.dimkich.integration.testing.storage.sql.table.TableSet;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Objects;

/**
 * One immutable mutation of the storage. Revert data is stored in a flat array indexed by
 * {@link Table#getId()}; the touched-table set is the compact, registry-bound table set used by the
 * planner.
 * <p>
 * The planner resolves the impact of a SUT mutation lazily: it analyzes the audit rows and counts
 * the physical rollback cost only when a transition asks for the exact cost, and it renders the
 * revert SQL only when the rollback commands are requested.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class StorageMutation {
    private final String script; // null for a SUT mutation

    private final TableSet touchedTables;

    @Getter(AccessLevel.NONE)
    private final TableMap<MutationImpact> impacts;

    public static StorageMutationBuilder builder(TableRegistry registry) {
        return new StorageMutationBuilder(registry);
    }

    public boolean isSutMutation() {
        return script == null;
    }

    private MutationImpact getImpact(Table table) {
        return impacts.get(table);
    }

    public boolean cannotRevert(Table table) {
        MutationImpact impact = getImpact(table);
        return impact == null || !impact.canRevert();
    }

    public List<String> getRevertSql(Table table) {
        MutationImpact impact = getImpact(table);
        return impact == null ? List.of() : impact.getRevertSql();
    }

    long getUndoCost(Table table) {
        MutationImpact impact = getImpact(table);
        return impact == null ? Long.MAX_VALUE : impact.getUndoCost();
    }

    /**
     * Cheap upper bound used by the planner before resolving the exact audit impact.
     */
    long getMaxUndoRows(Table table) {
        MutationImpact impact = getImpact(table);
        return impact == null ? Long.MAX_VALUE : impact.getMaxUndoRows();
    }

    /**
     * SUT mutations are compared by identity: only the very same mutation may be kept between
     * parts.
     */
    public boolean isSameMutation(StorageMutation other) {
        if (this == other) {
            return true;
        }
        if (isSutMutation() || other.isSutMutation()) {
            return false;
        }
        return Objects.equals(script, other.script);
    }

    public static final class StorageMutationBuilder {
        private String script;
        private final TableSet touchedTables;
        private final TableMap<MutationImpact> impacts = new TableMap<>();

        private StorageMutationBuilder(TableRegistry registry) {
            this.touchedTables = TableSet.empty(registry);
        }

        public StorageMutationBuilder script(String script) {
            this.script = script;
            return this;
        }

        public StorageMutationBuilder impact(Table table, MutationImpact impact) {
            touchedTables.add(table);
            impacts.put(table, impact);
            return this;
        }

        public StorageMutation build() {
            return new StorageMutation(script, touchedTables, impacts);
        }
    }
}
