package io.github.dimkich.integration.testing.postgresql.model.change;

/**
 * Logical change of one raw row key between the initial and final images of a change batch.
 */
public enum PostgresRowChange {
    UNCHANGED,
    INSERT,
    DELETE,
    UPDATE,
    REINSERT
}
