package io.github.dimkich.integration.testing.postgresql.service.journal;

import io.github.dimkich.integration.testing.postgresql.repository.PostgresJournalRepository;
import io.github.dimkich.integration.testing.storage.sql.table.TableSet;
import lombok.RequiredArgsConstructor;

import java.sql.SQLException;

/**
 * Owns the audit journal lifecycle: schema initialization, trigger attachment and reset. Reading
 * the journal is handled by {@link PostgresJournalReader}; JDBC and SQL live in
 * {@link PostgresJournalRepository}.
 */
@RequiredArgsConstructor
public class PostgresJournalLifecycle {
    private final PostgresJournalRepository journalRepository;
    private boolean auditInitialized;
    private TableSet attachedTables;

    void initialize() throws SQLException {
        if (auditInitialized) {
            return;
        }
        journalRepository.initialize();
        auditInitialized = true;
    }

    public void attachTriggers(TableSet requestedTables) throws SQLException {
        initialize();
        if (requestedTables.equals(attachedTables)) {
            return;
        }
        if (requestedTables.isEmpty()) {
            attachedTables = requestedTables.copy();
            return;
        }
        journalRepository.attachTriggers(requestedTables);
        attachedTables = requestedTables.copy();
    }

    public void invalidate() {
        attachedTables = null;
        auditInitialized = false;
    }

    public void resetJournal() throws SQLException {
        invalidate();
        initialize();
        journalRepository.truncate();
    }
}
