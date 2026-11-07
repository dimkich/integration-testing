package io.github.dimkich.integration.testing.storage.sql.state;

import lombok.Getter;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Exact revert data of one table inside a mutation.
 * <p>
 * {@code undoRows} is the number of physical row operations needed for the rollback. It is not
 * the number of SQL statements: one statement may affect many rows, while a primary-key change may
 * require a deletion and an insertion for one logical update.
 * <p>
 * The revert commands are rendered lazily. Exact data is often needed only to compare the
 * rollback cost with a fixture reload, and SQL must not be generated for that comparison.
 */
public final class ExactMutationImpact {
    @Getter
    private final int undoRows;
    private Supplier<List<String>> revertSqlSupplier;
    private List<String> revertSql;

    public ExactMutationImpact(int undoRows, List<String> revertSql) {
        this.undoRows = validateUndoRows(undoRows);
        this.revertSqlSupplier = null;
        this.revertSql = Objects.requireNonNull(revertSql, "revertSql");
    }

    public ExactMutationImpact(int undoRows, Supplier<List<String>> revertSqlSupplier) {
        this.undoRows = validateUndoRows(undoRows);
        this.revertSqlSupplier = Objects.requireNonNull(revertSqlSupplier, "revertSqlSupplier");
    }

    public List<String> getRevertSql() {
        if (revertSql == null) {
            revertSql = Objects.requireNonNull(
                    revertSqlSupplier.get(), "revertSql supplier result");
            revertSqlSupplier = null;
        }
        return revertSql;
    }

    private static int validateUndoRows(int undoRows) {
        if (undoRows < 0) {
            throw new IllegalArgumentException("undoRows must not be negative");
        }
        return undoRows;
    }
}
