package io.github.dimkich.integration.testing.postgresql.model.metadata;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * Diff-visible before/after values of one audit record. Ignored columns are already omitted and
 * nested value exclusions are applied.
 */
@Getter
@RequiredArgsConstructor
public final class PostgresRowValues {
    private final Map<String, Object> oldData;
    private final Map<String, Object> newData;

    /**
     * Whether the record has a before image.
     */
    public boolean hasOldData() {
        return oldData != null;
    }

    /**
     * Whether the record has an after image.
     */
    public boolean hasNewData() {
        return newData != null;
    }
}
