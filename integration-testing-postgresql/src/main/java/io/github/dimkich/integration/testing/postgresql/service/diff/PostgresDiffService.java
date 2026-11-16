package io.github.dimkich.integration.testing.postgresql.service.diff;

import io.github.dimkich.integration.testing.execution.StubAnswer;
import io.github.dimkich.integration.testing.postgresql.model.change.PostgresChangeBatch;
import io.github.dimkich.integration.testing.storage.diff.ObjectsDifference;
import io.github.dimkich.integration.testing.storage.diff.model.DiffEntry;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Builds the difference for the SUT part of a change batch. Reading the journal, resolving tags
 * and planning reverts are outside this class.
 */
@RequiredArgsConstructor
public class PostgresDiffService {
    private final ObjectsDifference objectsDifference;

    public List<DiffEntry> calculate(PostgresChangeBatch batch) {
        if (batch.getRecords().isEmpty()) {
            return null;
        }
        PostgresDiff diff = PostgresDiff.from(batch);
        if (diff.getBefore().isEmpty() && diff.getAfter().isEmpty()) {
            return null;
        }
        return StubAnswer.enable(() -> objectsDifference.getDifference(diff.getBefore(), diff.getAfter()));
    }
}
