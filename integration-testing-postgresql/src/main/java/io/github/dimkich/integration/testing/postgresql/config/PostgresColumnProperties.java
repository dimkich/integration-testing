package io.github.dimkich.integration.testing.postgresql.config;

import io.github.dimkich.integration.testing.serde.StandardSerdeProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Set;

/**
 * Per-column PostgreSQL configuration: optional serde overrides plus storage-diff exclusions.
 * <p>
 * A column may be excluded from the diff completely with {@link #ignore}, or its nested value
 * fields may be excluded with dot-separated {@link #excludedFields} paths. Both settings are
 * combined with the table metadata and affect only the calculated difference; audit and Smart
 * Revert still operate on the complete column values. Primary-key columns cannot use either
 * setting: they identify a row, so metadata preparation fails.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PostgresColumnProperties extends StandardSerdeProperties {
    /**
     * Excludes the whole column from the storage difference.
     */
    private boolean ignore;

    /**
     * Dot-separated paths inside the column value excluded from the storage difference.
     */
    private Set<String> excludedFields;
}
