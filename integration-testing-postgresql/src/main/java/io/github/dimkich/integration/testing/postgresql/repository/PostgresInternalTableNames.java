package io.github.dimkich.integration.testing.postgresql.repository;

import lombok.experimental.UtilityClass;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Names of the PostgreSQL tables owned by the testing infrastructure. They are not part of the
 * application schema and must be excluded from metadata and fixture processing. The audit journal
 * gets a process-unique suffix so concurrently running test JVMs do not share the same table.
 */
@UtilityClass
public class PostgresInternalTableNames {
    /**
     * Name of the legacy audit log table kept only for filtering old databases.
     */
    public static final String LEGACY_AUDIT_LOG = "_it_audit_log";
    /**
     * Prefix of the per-JVM audit journal table.
     */
    public static final String AUDIT_JOURNAL_PREFIX = "_it_postgres_journal_";
    /**
     * Current audit journal table name with a process-unique suffix.
     */
    public static final String AUDIT_JOURNAL = AUDIT_JOURNAL_PREFIX + randomSuffix();

    private static String randomSuffix() {
        return String.format("%016x", ThreadLocalRandom.current().nextLong());
    }
}
