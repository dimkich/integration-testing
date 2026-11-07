package io.github.dimkich.integration.testing.postgresql.service.change;

import io.github.dimkich.integration.testing.postgresql.model.change.PostgresChangeBatch;
import io.github.dimkich.integration.testing.postgresql.model.change.PostgresJournalRecord;
import io.github.dimkich.integration.testing.postgresql.service.diff.PostgresDiffService;
import io.github.dimkich.integration.testing.postgresql.service.journal.PostgresJournalReader;
import io.github.dimkich.integration.testing.storage.sql.StorageChanges;
import io.github.dimkich.integration.testing.storage.sql.table.TableRegistry;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Thin facade over the change pipeline: reads the journal, keeps the tags of the last SQL
 * transition and exposes lazy diff and mutation builders to the state engine. A tag is the index
 * of the statement in the tagged SQL array.
 */
@RequiredArgsConstructor
public class PostgresChangePipeline {
    // The order of the fields is coupled to the bean definition in PostgresConfig.PostProcessor.
    private final PostgresJournalReader journalReader;
    private final PostgresDiffService diffService;
    private final PostgresMutationAssembler mutationAssembler;
    private PostgresTaggedSql taggedSql = PostgresTaggedSql.empty();

    public void setTaggedSql(String[] taggedSql) {
        this.taggedSql = this.taggedSql.with(taggedSql);
    }

    public void assertNoPendingTaggedSql() {
        taggedSql.assertEmpty();
    }

    public StorageChanges getStorageChanges(TableRegistry registry, String postReadSql) throws Exception {
        List<PostgresJournalRecord> records = journalReader.readAndAdvanceWatermark(registry, postReadSql);
        PostgresChangeBatch batch = new PostgresChangeBatch(records);
        String[] taggedSql = this.taggedSql.consume();
        this.taggedSql = PostgresTaggedSql.empty();
        return StorageChanges.of(
                () -> diffService.calculate(batch),
                () -> mutationAssembler.assemble(batch, taggedSql, registry)
        );
    }

    public void reset() {
        journalReader.resetWatermark();
        taggedSql = PostgresTaggedSql.empty();
    }
}
