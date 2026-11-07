package io.github.dimkich.integration.testing.storage.sql.state;

import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * Ordered log of the global storage mutations. Owns the operations that only reshape or fill the
 * log; the baseline stays in {@link StorageState}.
 */
@ToString(of = {"mutations", "confirmedMutations"})
class StorageMutationLog {
    private final ArrayList<StorageMutation> mutations;

    /**
     * Number of leading mutations that are confirmed and can be kept without replaying. The
     * mutations after this boundary are removed after the rollback or reload.
     */
    private int confirmedMutations;

    StorageMutationLog() {
        this(0);
    }

    StorageMutationLog(int initialCapacity) {
        mutations = new ArrayList<>(Math.max(0, initialCapacity));
    }

    int size() {
        return mutations.size();
    }

    StorageMutation get(int index) {
        return mutations.get(index);
    }

    void add(StorageMutation mutation) {
        mutations.add(mutation);
    }

    void merge(StorageMutationLog other) {
        int oldSize = mutations.size();
        boolean prefixConfirmed = confirmedMutations == oldSize;
        mutations.ensureCapacity(oldSize + other.mutations.size());
        mutations.addAll(other.mutations);
        if (prefixConfirmed) {
            confirmedMutations = oldSize + Math.min(other.confirmedMutations, other.mutations.size());
        }
    }

    /**
     * Replaces the unconfirmed tail with the already applied mutations and confirms the new tail.
     */
    void applyMutations(List<StorageMutation> appliedMutations) {
        mutations.subList(confirmedMutations, mutations.size()).clear();
        mutations.addAll(appliedMutations);
        confirmedMutations = mutations.size();
    }

    /**
     * Builds the log of the next state without mutating either log: the confirmed prefix comes from
     * this log, the remaining mutations come from the target.
     */
    StorageMutationLog transitionTo(StorageMutationLog target, int prefixSize) {
        StorageMutationLog result = new StorageMutationLog(target.size());
        result.mutations.addAll(mutations.subList(0, prefixSize));
        result.mutations.addAll(target.mutations.subList(prefixSize, target.mutations.size()));
        result.confirmedMutations = prefixSize;
        return result;
    }

    void copyTo(StorageMutationLog target) {
        target.mutations.ensureCapacity(target.mutations.size() + mutations.size());
        target.mutations.addAll(mutations);
        target.confirmedMutations = confirmedMutations;
    }
}
