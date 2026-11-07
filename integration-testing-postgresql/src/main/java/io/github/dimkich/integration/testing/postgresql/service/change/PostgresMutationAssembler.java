package io.github.dimkich.integration.testing.postgresql.service.change;

import io.github.dimkich.integration.testing.postgresql.model.change.PostgresChangeBatch;
import io.github.dimkich.integration.testing.postgresql.model.change.PostgresJournalRecord;
import io.github.dimkich.integration.testing.postgresql.model.change.PostgresTableState;
import io.github.dimkich.integration.testing.postgresql.model.revert.PostgresMutationImpactFactory;
import io.github.dimkich.integration.testing.storage.sql.state.StorageMutation;
import io.github.dimkich.integration.testing.storage.sql.table.TableMap;
import io.github.dimkich.integration.testing.storage.sql.table.TableRegistry;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Assembles the storage mutations of a change batch. SQL inits run before a test, so tagged SQL
 * records come first and the remaining untagged records are SUT mutations. SUT mutations stay one
 * per table so that the planner does not couple unrelated tables. Revert impacts are registered as
 * lazy suppliers: their analysis is resolved when a transition needs the count, and their SQL is
 * rendered only for an actual rollback.
 * <p>
 * SUT impacts reuse the table states of the batch, which are also used by the diff. Tagged SQL
 * records get their own states because they are not part of the diff.
 */
@RequiredArgsConstructor
public class PostgresMutationAssembler {
    // The order of the fields is coupled to the bean definition in PostgresConfig.PostProcessor.
    private final PostgresMutationImpactFactory mutationImpactFactory;

    /**
     * Assembles mutations for one change batch.
     *
     * @param taggedSql statements with their array indices as tags, or {@code null} when there is
     *                  no tagged SQL
     */
    public List<StorageMutation> assemble(PostgresChangeBatch batch,
                                          String[] taggedSql,
                                          TableRegistry registry) {
        int tagCount = taggedSql == null ? 0 : taggedSql.length;
        TableMap<PostgresTableState> sutTables = batch.getSutTables();
        int sutCapacity = Math.min(registry.size(), batch.getRecords().size());
        List<StorageMutation> mutations = new ArrayList<>(tagCount + sutCapacity);
        Iterator<PostgresJournalRecord> iterator = batch.getRecords().iterator();
        PostgresJournalRecord current = nextRecord(iterator);

        for (int tag = 0; tag < tagCount; tag++) {
            PostgresTaggedMutation builder = new PostgresTaggedMutation(taggedSql[tag], registry);
            while (current != null && current.isTaggedWith(tag)) {
                builder.add(current);
                current = nextRecord(iterator);
            }
            mutations.add(builder.build(mutationImpactFactory));
        }

        while (current != null) {
            if (current.isTagged()) {
                throw new IllegalStateException("Tagged SQL record with tag " + current.getNode()
                        + " appears after SUT records");
            }
            current = nextRecord(iterator);
        }

        sutTables.forEach(state -> mutations.add(
                state.toMutation(registry, mutationImpactFactory.create(state))));
        return mutations;
    }

    private static PostgresJournalRecord nextRecord(Iterator<PostgresJournalRecord> iterator) {
        return iterator.hasNext() ? iterator.next() : null;
    }
}
