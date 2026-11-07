package io.github.dimkich.integration.testing.storage.sql.state;

import io.github.dimkich.integration.testing.dbunit.DbUnitFixture;
import io.github.dimkich.integration.testing.storage.sql.table.Table;
import io.github.dimkich.integration.testing.storage.sql.table.TableMap;
import io.github.dimkich.integration.testing.storage.sql.table.TableRegistry;
import io.github.dimkich.integration.testing.storage.sql.table.TableSet;
import lombok.RequiredArgsConstructor;

/**
 * Analyzes the source and target states and accumulates the transition decision for every table.
 * Instances are reused for consecutive transitions; call {@link #reset(DbUnitFixture)} before each
 * analysis.
 */
@RequiredArgsConstructor
public final class TransitionAnalyzer {
    private final TableRegistry registry;
    private final TableMap<TableTransition> tables = new TableMap<>();
    private TableSet allTables;
    private int keptMutations;

    public void reset(DbUnitFixture fixture) {
        if (allTables == null || allTables.size() != registry.size()) {
            allTables = registry.getAllTables();
        }
        for (Table table : allTables) {
            tables.computeIfAbsent(table, t -> new TableTransition())
                    .reset(fixture.getRowCount(table));
        }
        keptMutations = 0;
    }

    void analyzeStructuralPrefix(StorageMutationLog source, StorageMutationLog target) {
        int structuralPrefix = 0;
        while (structuralPrefix < source.size() && structuralPrefix < target.size()) {
            StorageMutation sourceMutation = source.get(structuralPrefix);
            StorageMutation targetMutation = target.get(structuralPrefix);
            if (!sourceMutation.isSameMutation(targetMutation)) {
                break;
            }
            recordTouch(sourceMutation.getTouchedTables(), structuralPrefix);
            recordTouch(targetMutation.getTouchedTables(), structuralPrefix);
            structuralPrefix++;
        }
        for (Table table : allTables) {
            TableTransition transition = tables.getRequired(table);
            transition.recordTouch(structuralPrefix);
        }
        keptMutations = structuralPrefix;
    }

    public void evaluateBaselineChanges(TableBaseline source, TableBaseline target) {
        for (Table table : allTables) {
            if (!target.differsFrom(source, table)) {
                continue;
            }
            TableTransition transition = tables.getRequired(table);
            keptMutations = transition.applyBaseReset(keptMutations);
        }
    }

    void evaluateRollbackCapabilities(StorageMutationLog source, StorageMutationLog target) {
        for (int i = source.size() - 1; i >= 0; i--) {
            StorageMutation mutation = source.get(i);
            for (Table table : mutation.getTouchedTables()) {
                TableTransition transition = tables.getRequired(table);
                if (transition.needsBaseReset()) {
                    continue;
                }
                if (mutation.cannotRevert(table)) {
                    transition.rejectRevertAt(i + 1);
                    continue;
                }
                transition.accountUndoUpperBound(mutation.getMaxUndoRows(table));
            }
        }

        for (int i = 0; i < target.size(); i++) {
            StorageMutation mutation = target.get(i);
            if (!mutation.isSutMutation()) {
                continue;
            }
            for (Table table : mutation.getTouchedTables()) {
                TableTransition transition = tables.getRequired(table);
                if (!transition.needsBaseReset()) {
                    transition.rejectRevertAt(i + 1);
                }
            }
        }

        TableMap<Long> exactUndoRows = new TableMap<>();
        TableSet stopped = TableSet.empty(registry);
        for (int i = source.size() - 1; i >= 0; i--) {
            StorageMutation mutation = source.get(i);
            for (Table table : mutation.getTouchedTables()) {
                TableTransition transition = tables.getRequired(table);
                if (stopped.contains(table) || !transition.needsExactUndoCost()
                        || mutation.cannotRevert(table)) {
                    continue;
                }
                Long previousRows = exactUndoRows.get(table);
                long undoRows = (previousRows == null ? 0L : previousRows)
                        + mutation.getUndoCost(table);
                exactUndoRows.put(table, undoRows);
                if (transition.isReloadCheaper(undoRows)) {
                    transition.rejectRevertAt(i + 1);
                    stopped.add(table);
                }
            }
        }
    }

    public int resolveKeptMutations() {
        boolean changed;
        do {
            changed = false;
            for (Table table : allTables) {
                TableTransition transition = tables.getRequired(table);
                if (!transition.shouldReset(keptMutations)) {
                    continue;
                }
                keptMutations = transition.applyBaseReset(keptMutations);
                changed = true;
            }
        } while (changed);
        return keptMutations;
    }

    public boolean needsReset(Table table) {
        TableTransition transition = tables.getRequired(table);
        return transition.needsBaseReset();
    }

    TableSet getAllTables() {
        return allTables;
    }

    private void recordTouch(TableSet touched, int mutationIndex) {
        for (Table table : touched) {
            TableTransition transition = tables.getRequired(table);
            transition.recordTouch(mutationIndex);
        }
    }
}
