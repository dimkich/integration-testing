package io.github.dimkich.integration.testing.postgresql.service.change;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * Tagged SQL statements of the last transition. A tag is the index of the statement in the array.
 * The array is owned by the pipeline and consumed exactly once when the audit journal is read.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
final class PostgresTaggedSql {
    private static final PostgresTaggedSql EMPTY = new PostgresTaggedSql(null);

    private final String[] sql;

    static PostgresTaggedSql empty() {
        return EMPTY;
    }

    PostgresTaggedSql with(String[] sql) {
        if (sql == null || sql.length == 0) {
            return this;
        }
        assertEmpty();
        return new PostgresTaggedSql(sql);
    }

    void assertEmpty() {
        if (this.sql != null) {
            throw new IllegalStateException(
                    "Previous tagged SQL transition has not been read from the audit journal");
        }
    }

    String[] consume() {
        return sql;
    }
}
