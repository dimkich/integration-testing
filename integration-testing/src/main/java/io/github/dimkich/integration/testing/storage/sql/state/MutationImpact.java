package io.github.dimkich.integration.testing.storage.sql.state;

import lombok.Getter;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Impact of one table inside a mutation.
 * <p>
 * The cheap part of the impact is available immediately: {@link #canRevert()} and
 * {@link #getMaxUndoRows()} do not resolve audit data. The exact impact is resolved on demand and is
 * memoized.
 * <p>
 * {@code maxUndoRows} is a safe upper bound in physical row operations. It uses the fact that one
 * {@code UPDATE} audit record can require at most two operations on rollback: a plain update, or a
 * delete plus insert when the primary key or an identity-always column changed.
 */
public final class MutationImpact {
    private static final MutationImpact NON_REVERTIBLE = new MutationImpact(0, null);

    @Getter
    private final int maxUndoRows;
    private Supplier<ExactMutationImpact> exactSupplier;

    private ExactMutationImpact exact;

    private MutationImpact(int maxUndoRows, Supplier<ExactMutationImpact> exactSupplier) {
        this.maxUndoRows = validateRows(maxUndoRows);
        this.exactSupplier = exactSupplier;
    }

    /**
     * Returns the shared impact that cannot be rolled back.
     */
    public static MutationImpact nonRevertible() {
        return NON_REVERTIBLE;
    }

    /**
     * Creates a revertible impact whose exact data is analyzed only when it is actually needed.
     */
    public static MutationImpact revertible(int maxUndoRows,
                                            Supplier<ExactMutationImpact> exactSupplier) {
        return new MutationImpact(maxUndoRows,
                Objects.requireNonNull(exactSupplier, "exactSupplier"));
    }

    public boolean canRevert() {
        return exactSupplier != null || exact != null;
    }

    /**
     * Physical row operations needed for the rollback. The exact impact is resolved lazily.
     */
    public long getUndoCost() {
        return canRevert() ? resolveExact().getUndoRows() : Long.MAX_VALUE;
    }

    public List<String> getRevertSql() {
        ExactMutationImpact impact = resolveExact();
        return impact == null ? List.of() : impact.getRevertSql();
    }

    private ExactMutationImpact resolveExact() {
        if (exact == null && exactSupplier != null) {
            ExactMutationImpact resolved = Objects.requireNonNull(
                    exactSupplier.get(), "exact impact supplier result");
            if (resolved.getUndoRows() > maxUndoRows) {
                throw new IllegalStateException("Exact undo rows " + resolved.getUndoRows()
                        + " exceed the declared upper bound " + maxUndoRows);
            }
            exact = resolved;
            exactSupplier = null;
        }
        return exact;
    }

    private static int validateRows(int rows) {
        if (rows < 0) {
            throw new IllegalArgumentException("rows must not be negative");
        }
        return rows;
    }
}
