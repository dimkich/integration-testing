package io.github.dimkich.integration.testing.storage.sql.state;

/**
 * Computed transition state of one table. The analyzer accumulates the facts here instead of
 * keeping parallel arrays. Instances are reused; {@link #reset(int)} clears the state of the
 * previous analysis.
 */
final class TableTransition {
    private int fixtureSize;

    private int firstTouch = Integer.MAX_VALUE;
    private int minPrefix;
    private long undoUpperBound;
    private boolean baseResetRequired;

    void reset(int fixtureSize) {
        this.fixtureSize = fixtureSize;
        firstTouch = Integer.MAX_VALUE;
        minPrefix = 0;
        undoUpperBound = 0;
        baseResetRequired = false;
    }

    void recordTouch(int mutationIndex) {
        firstTouch = Math.min(firstTouch, mutationIndex);
    }

    void accountUndoUpperBound(long maxUndoRows) {
        if (undoUpperBound > Long.MAX_VALUE - maxUndoRows) {
            undoUpperBound = Long.MAX_VALUE;
            return;
        }
        undoUpperBound += maxUndoRows;
    }

    boolean needsExactUndoCost() {
        return exceedsFixtureSize(undoUpperBound);
    }

    boolean isReloadCheaper(long undoRows) {
        return exceedsFixtureSize(undoRows);
    }

    void rejectRevertAt(int boundary) {
        minPrefix = Math.max(minPrefix, boundary);
    }

    boolean shouldReset(int keptMutations) {
        return !baseResetRequired && minPrefix > keptMutations;
    }

    int applyBaseReset(int keptMutations) {
        baseResetRequired = true;
        return Math.min(keptMutations, firstTouch);
    }

    boolean needsBaseReset() {
        return baseResetRequired;
    }

    private boolean exceedsFixtureSize(long rows) {
        return fixtureSize >= 0 && rows >= fixtureSize + 1L;
    }
}
