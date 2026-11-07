package io.github.dimkich.integration.testing;

/**
 * Interface for test data storage implementations used in integration testing.
 * <p>
 * This interface provides a contract for detecting data changes produced by a test.
 * Implementations fall into two categories:
 * <ul>
 *   <li><b>Snapshot storages</b> (Redis, in-memory caches) capture the state before the test
 *       and compare it with the state after the test. See
 *       {@link io.github.dimkich.integration.testing.storage.SnapshotTestDataStorage}.</li>
 *   <li><b>Delta storages</b> (databases with audit logs, Kafka, event-driven systems) know
 *       their changes without a full scan and return the diff directly.</li>
 * </ul>
 *
 * @author dimkich
 */
public interface TestDataStorage {
    /**
     * Returns the unique name identifier for this test data storage.
     * <p>
     * The name is used to identify and retrieve specific storage instances
     * from the storage registry and to look up storage specific configuration.
     * </p>
     *
     * @return the name of this test data storage, never {@code null}
     */
    String getName();

    /**
     * Prepares this storage for the upcoming test.
     * <p>
     * Called by the framework once before each test, after initialization has been applied
     * and all asynchronous work has settled. The default implementation does nothing;
     * implementations that need a refreshed baseline, such as snapshot storages, override it.
     * </p>
     *
     * @throws Exception if an error occurs while preparing the storage
     */
    default void prepareForTest() throws Exception {
    }

    /**
     * Computes the difference for this storage after the test has been executed.
     * <p>
     * For snapshot storages the difference is computed against the snapshot captured by
     * {@link io.github.dimkich.integration.testing.storage.SnapshotTestDataStorage#prepareForTest()}
     * before the test. For delta storages the storage knows its own changes and returns them
     * directly.
     * </p>
     *
     * @return the computed difference (a
     * {@link io.github.dimkich.integration.testing.storage.mapping.Container}) or {@code null}
     * if there were no changes
     * @throws Exception if an error occurs while computing the difference
     */
    Object getDiff() throws Exception;
}
