package io.github.dimkich.integration.testing.storage;

import io.github.dimkich.integration.testing.TestDataStorage;
import io.github.dimkich.integration.testing.execution.StubAnswer;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * Base class for snapshot based {@link TestDataStorage} implementations.
 * <p>
 * A snapshot storage cannot know its own changes, so the framework captures the state
 * before the test via {@link #prepareForTest()} and the storage compares it with the state
 * after the test inside {@link #getDiff()}. Subclasses only need to implement
 * {@link #doGetCurrentValue()} - the storage specific way to read the full snapshot
 * (for example, an in-memory Redis mirror or a {@code SELECT *} over a set of tables).
 * </p>
 *
 * @see io.github.dimkich.integration.testing.TestDataStorage
 */
@RequiredArgsConstructor
public abstract class SnapshotTestDataStorage implements TestDataStorage {
    private final ObjectsDifference objectsDifference;

    private Map<String, Object> currentValue = Map.of();
    private boolean affected = true;

    /**
     * Reads the full snapshot of this storage.
     *
     * @return a map representing the current state of all data
     * @throws Exception if an error occurs while reading the data
     */
    @SuppressWarnings("RedundantThrows")
    protected abstract Map<String, Object> doGetCurrentValue() throws Exception;

    /**
     * Marks this storage as changed so that the next {@link #prepareForTest()} refreshes the baseline.
     * Subclasses must call this from every operation that mutates their data. Code that mutates the
     * storage state without going through its API must call it explicitly.
     */
    public final void markAffected() {
        affected = true;
    }

    @Override
    public final void prepareForTest() throws Exception {
        if (!affected) {
            return;
        }
        this.currentValue = StubAnswer.enable(this::doGetCurrentValue);
        affected = false;
    }

    @Override
    public Object getDiff() throws Exception {
        Map<String, Object> previousValue = this.currentValue;
        this.currentValue = StubAnswer.enable(this::doGetCurrentValue);
        affected = false;
        return StubAnswer.enable(() -> objectsDifference.getDifference(getName(), previousValue, currentValue));
    }
}
