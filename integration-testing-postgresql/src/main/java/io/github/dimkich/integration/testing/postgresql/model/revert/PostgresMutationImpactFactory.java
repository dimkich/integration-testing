package io.github.dimkich.integration.testing.postgresql.model.revert;

import io.github.dimkich.integration.testing.postgresql.model.change.PostgresTableState;
import io.github.dimkich.integration.testing.storage.sql.state.MutationImpact;
import lombok.RequiredArgsConstructor;

/**
 * Creates the revert impact of one table from its audit state. The exact operations are analyzed
 * only when the factory cannot prove that rolling back is cheaper from the cheap physical-operation
 * upper bound.
 */
@RequiredArgsConstructor
public class PostgresMutationImpactFactory {
    // The order of the fields is coupled to the bean definition in PostgresConfig.PostProcessor.
    private final PostgresExactMutationImpactFactory exactMutationImpactFactory;

    public MutationImpact create(PostgresTableState state) {
        return MutationImpact.revertible(state.getMaxUndoRows(),
                () -> exactMutationImpactFactory.create(state));
    }
}
