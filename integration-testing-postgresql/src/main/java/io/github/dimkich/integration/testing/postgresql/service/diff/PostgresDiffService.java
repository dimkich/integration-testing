package io.github.dimkich.integration.testing.postgresql.service.diff;

import io.github.dimkich.integration.testing.execution.StubAnswer;
import io.github.dimkich.integration.testing.postgresql.model.change.PostgresChangeBatch;
import io.github.dimkich.integration.testing.storage.ObjectsDifference;
import lombok.RequiredArgsConstructor;

/**
 * Builds the difference for the SUT part of a change batch. Reading the journal, resolving tags
 * and planning reverts are outside this class.
 */
@RequiredArgsConstructor
public class PostgresDiffService {
    // The order of the fields is coupled to the bean definition in PostgresConfig.PostProcessor.
    private final String storageName;
    private final ObjectsDifference objectsDifference;

    public Object calculate(PostgresChangeBatch batch) {
        if (batch.getRecords().isEmpty()) {
            return null;
        }
        PostgresDiff diff = PostgresDiff.from(batch);
        if (diff.getBefore().isEmpty() && diff.getAfter().isEmpty()) {
            return null;
        }
        return StubAnswer.enable(() -> objectsDifference.getDifference(
                storageName, diff.getBefore(), diff.getAfter()));
    }
}
