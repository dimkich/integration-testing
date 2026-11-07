package io.github.dimkich.integration.testing.postgresql.model.change;

import io.github.dimkich.integration.testing.postgresql.model.metadata.PostgresRowValues;
import io.github.dimkich.integration.testing.postgresql.model.metadata.PostgresTableMetadata;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.nio.ByteBuffer;

/**
 * Initial and final images of one raw row key. One state replaces all journal records that touched
 * the key: only the first before image and the last after image are kept.
 * <p>
 * The state is intentionally keyed by the raw row key, not by a physical row identity. If a primary
 * key changes, the old and the new key become two independent states.
 * <p>
 * Converted row values are cached lazily: the difference engine and the display keys share one
 * conversion per state. The state is not thread-safe.
 */
@Getter
@RequiredArgsConstructor
public final class PostgresRowState {
    private final PostgresTableMetadata metadata;
    private final PostgresRawRow before;
    private final PostgresRawRow after;
    @Getter(AccessLevel.NONE)
    private PostgresRowValues values;
    @Getter(AccessLevel.NONE)
    private PostgresRowChange change;

    /**
     * Logical change represented by the two endpoint images.
     */
    public PostgresRowChange getChange() {
        if (change == null) {
            change = computeChange();
        }
        return change;
    }

    private PostgresRowChange computeChange() {
        if (before == null && after == null) {
            throw new IllegalStateException("Row state has neither before nor after image");
        }
        if (before == null) {
            return PostgresRowChange.INSERT;
        }
        if (after == null) {
            return PostgresRowChange.DELETE;
        }
        if (isUnchanged()) {
            return PostgresRowChange.UNCHANGED;
        }
        if (isReinsertRequired()) {
            return PostgresRowChange.REINSERT;
        }
        return PostgresRowChange.UPDATE;
    }

    public boolean isChanged() {
        return getChange() != PostgresRowChange.UNCHANGED;
    }

    /**
     * Number of physical row operations needed to undo this change.
     */
    public int getUndoRowCount() {
        return switch (getChange()) {
            case UNCHANGED -> 0;
            case INSERT, DELETE -> 1;
            case REINSERT -> 2;
            case UPDATE -> metadata.hasUpdateColumns() ? 1 : 0;
        };
    }

    /**
     * Whether the row did not change between the start and the end of the batch. A key that was
     * inserted and then deleted has no state at all and is not represented by this object.
     */
    public boolean isUnchanged() {
        return before != null && before.equals(after);
    }

    /**
     * Whether the transition between the two endpoint images cannot be reverted by a plain update.
     */
    public boolean isReinsertRequired() {
        return before != null && after != null
                && (!before.getRowKey().equals(after.getRowKey())
                || !before.hasSameIdentityAlways(after));
    }

    /**
     * Diff-visible values of the endpoint images. Fields that are equal in both images are
     * converted once and shared.
     */
    public PostgresRowValues getValues() {
        if (values == null) {
            ByteBuffer[] oldFields = before == null ? null : before.getFields();
            ByteBuffer[] newFields = after == null ? null : after.getFields();
            values = metadata.convert(oldFields, newFields);
        }
        return values;
    }

    public String getBeforeDisplayKey() {
        return before == null ? null : before.getDisplayKey(getValues().getOldData());
    }

    public String getAfterDisplayKey() {
        return after == null ? null : after.getDisplayKey(getValues().getNewData());
    }
}
