package io.github.dimkich.integration.testing.postgresql.service.change;

import io.github.dimkich.integration.testing.postgresql.model.change.PostgresJournalRecord;
import io.github.dimkich.integration.testing.postgresql.model.change.PostgresTableState;
import io.github.dimkich.integration.testing.postgresql.model.revert.PostgresMutationImpactFactory;
import io.github.dimkich.integration.testing.storage.sql.state.StorageMutation;
import io.github.dimkich.integration.testing.storage.sql.table.TableMap;
import io.github.dimkich.integration.testing.storage.sql.table.TableRegistry;

/**
 * Accumulates the audit records of one tagged SQL statement. The statement script is attached to
 * the resulting mutation; every touched table gets its own lazy revert impact.
 */
final class PostgresTaggedMutation {
    private final String script;
    private final TableRegistry registry;
    private final TableMap<PostgresTableState> states;

    PostgresTaggedMutation(String script, TableRegistry registry) {
        this.script = script;
        this.registry = registry;
        this.states = new TableMap<>(registry.size());
    }

    void add(PostgresJournalRecord record) {
        states.computeIfAbsent(record.getTable(), PostgresTableState::new).addRecord(record);
    }

    StorageMutation build(PostgresMutationImpactFactory impactFactory) {
        StorageMutation.StorageMutationBuilder builder = StorageMutation.builder(registry);
        if (script != null) {
            builder.script(script);
        }
        states.forEach(state -> builder.impact(state.getTable(), impactFactory.create(state)));
        return builder.build();
    }
}
