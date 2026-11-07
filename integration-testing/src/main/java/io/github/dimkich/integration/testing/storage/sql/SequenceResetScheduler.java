package io.github.dimkich.integration.testing.storage.sql;

import io.github.dimkich.integration.testing.Test;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Schedules sequence restarts around test boundaries and folds the pending restart into the next
 * audit read when possible.
 */
@RequiredArgsConstructor
public final class SequenceResetScheduler {
    private final SQLDataStorage storage;

    private boolean sequenceResetPending = true;

    void before(Test test) throws Exception {
        if (test.isContainer()) {
            return;
        }
        if (sequenceResetPending) {
            restartSequences();
        }
        sequenceResetPending = test.getType() != Test.Type.TestPart || test.isLastLeaf();
    }

    String pendingResetSql() {
        return sequenceResetPending ? storage.getSequenceRestartSql() : null;
    }

    void clearPending() {
        sequenceResetPending = false;
    }

    void reset() {
        sequenceResetPending = true;
    }

    private void restartSequences() throws Exception {
        String sql = storage.getSequenceRestartSql();
        if (StringUtils.hasText(sql)) {
            storage.executeSql(List.of(sql), List.of());
        }
    }
}
