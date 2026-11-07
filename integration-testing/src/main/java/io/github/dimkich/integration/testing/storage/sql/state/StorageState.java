package io.github.dimkich.integration.testing.storage.sql.state;

import io.github.dimkich.integration.testing.storage.sql.table.Table;
import io.github.dimkich.integration.testing.storage.sql.table.TableRegistry;
import lombok.AccessLevel;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@ToString(of = {"baseline", "storageMutationLog"})
public class StorageState {
    private final TableRegistry registry;
    private final TableBaseline baseline;
    @Setter(AccessLevel.PACKAGE)
    private StorageMutationLog storageMutationLog = new StorageMutationLog();

    public StorageState(TableRegistry registry) {
        this.registry = registry;
        this.baseline = new TableBaseline(registry);
    }

    public void setBaselineLoaded(Table table) {
        baseline.setLoaded(table);
    }

    public void setBaselineCleared(Table table) {
        baseline.setCleared(table);
    }

    /**
     * Adds one SQL block of an init declaration to the global mutation log.
     */
    public void addInitSql(String sql) {
        storageMutationLog.add(StorageMutation.builder(registry)
                .script(sql)
                .build());
    }

    public void merge(StorageState other) {
        baseline.merge(other.baseline);
        storageMutationLog.merge(other.storageMutationLog);
    }

    /**
     * Replaces and confirms the unconfirmed tail with all SQL and SUT mutations read from the
     * storage.
     */
    public void applyMutations(List<StorageMutation> appliedMutations) {
        storageMutationLog.applyMutations(appliedMutations);
    }

    /**
     * Records that the application has cleared the table outside the harness. The clear is added to
     * the confirmed history as a non-revertible SUT mutation, so the planner resets the table on the
     * next transition if that history is no longer part of the target state.
     */
    public void markTableCleared(Table table) {
        baseline.setCleared(table);
        storageMutationLog.applyMutations(List.of(StorageMutation.builder(registry)
                .impact(table, MutationImpact.nonRevertible())
                .build()));
    }

    /**
     * Builds the plan of the transition from this state to the target state.
     */
    public TransitionPlan planTransition(StorageState target, TransitionAnalyzer analyzer) {
        // 1. Structural common prefix and the first touch of every table.
        analyzer.analyzeStructuralPrefix(this.storageMutationLog, target.storageMutationLog);

        // 2. Baseline changes force a reset and may shorten the keepable prefix.
        analyzer.evaluateBaselineChanges(this.baseline, target.baseline);

        // 3. Rollback capability and cost define the minimum technically possible prefix.
        analyzer.evaluateRollbackCapabilities(this.storageMutationLog, target.storageMutationLog);

        // 4. Cascading reset resolution.
        int keptMutations = analyzer.resolveKeptMutations();

        TransitionPlan plan = new TransitionPlan(registry);

        // 5. Rollback in strict reverse order, only for tables without a baseline reset.
        for (int i = this.storageMutationLog.size() - 1; i >= keptMutations; i--) {
            StorageMutation mutation = this.storageMutationLog.get(i);
            for (Table table : mutation.getTouchedTables()) {
                if (analyzer.needsReset(table)) {
                    continue;
                }
                List<String> revertSql = mutation.getRevertSql(table);
                // A net-zero mutation may have no revert commands. Such a rollback changes nothing
                // and must not invalidate the table cache.
                if (!revertSql.isEmpty()) {
                    plan.markChanged(table);
                    plan.addRollbackSql(revertSql);
                }
            }
        }

        // 6. Baseline reset of the tables that cannot be rolled back.
        for (Table table : analyzer.getAllTables()) {
            if (!analyzer.needsReset(table)) {
                continue;
            }
            if (target.baseline.isLoaded(table)) {
                plan.load(table);
            } else {
                plan.clear(table);
            }
        }

        // 7. Forward SQL of the target tail, once per mutation.
        for (int i = keptMutations; i < target.storageMutationLog.size(); i++) {
            StorageMutation mutation = target.storageMutationLog.get(i);
            if (!mutation.isSutMutation()) {
                for (Table table : mutation.getTouchedTables()) {
                    plan.markChanged(table);
                }
                plan.addForwardSql(mutation.getScript());
            }
        }

        // 8. Build the resulting mutation log without mutating the target state.
        plan.setStorageMutationLog(this.storageMutationLog.transitionTo(target.storageMutationLog, keptMutations));
        return plan;
    }

    public boolean isBaselineLoaded(Table table) {
        return baseline.isLoaded(table);
    }

    public boolean isBaselineCleared(Table table) {
        return baseline.isCleared(table);
    }

    public StorageState copy() {
        StorageState copy = new StorageState(this.registry);
        copy.baseline.copyFrom(this.baseline);
        this.storageMutationLog.copyTo(copy.storageMutationLog);
        return copy;
    }
}
