package io.github.dimkich.integration.testing.postgresql.service.journal;

import io.github.dimkich.integration.testing.postgresql.model.change.PostgresJournalRecord;
import io.github.dimkich.integration.testing.postgresql.repository.PostgresJournalRepository;
import io.github.dimkich.integration.testing.storage.sql.table.TableRegistry;
import lombok.RequiredArgsConstructor;

import java.sql.SQLException;
import java.util.List;

/**
 * Advances the audit watermark and exposes the journal records read since the previous call. The
 * journal rows are mapped to rich {@link PostgresJournalRecord}s by the repository.
 */
@RequiredArgsConstructor
public class PostgresJournalReader {
    // The order of the fields is coupled to the bean definition in PostgresConfig.PostProcessor.
    private final PostgresJournalLifecycle journalLifecycle;
    private final PostgresJournalRepository journalRepository;

    private long lastReadId;

    public List<PostgresJournalRecord> readAndAdvanceWatermark(TableRegistry registry, String postReadSql)
            throws SQLException {
        journalLifecycle.initialize();
        List<PostgresJournalRecord> records = journalRepository.readAfter(lastReadId, registry,
                postReadSql);
        if (!records.isEmpty()) {
            lastReadId = records.get(records.size() - 1).getId();
        }
        return records;
    }

    public void resetWatermark() {
        lastReadId = 0;
    }
}
