package io.github.dimkich.integration.testing;

import java.util.Map;
import java.util.Set;

/**
 * Interface for test data storage implementations used in integration testing.
 * <p>
 * This interface provides a contract for managing and retrieving test data state
 * from various storage backends (e.g., SQL databases, key-value stores, Redis).
 * Implementations are responsible for providing the current state of their data
 * and optionally handling difference notifications when data changes are detected.
 * </p>
 *
 * @author dimkich
 */
public interface TestDataStorage {
    /**
     * Returns the unique name identifier for this test data storage.
     * <p>
     * The name is used to identify and retrieve specific storage instances
     * from the storage registry.
     * </p>
     *
     * @return the name of this test data storage, never {@code null}
     */
    String getName();

    /**
     * Retrieves the current state of the data stored in this storage.
     * <p>
     * The returned map represents the current snapshot of all data entities,
     * where keys are typically composite identifiers (e.g., "tableName_id" for SQL,
     * "keyspace_id" for key-value stores) and values are the actual data objects.
     * </p>
     * <p>
     * The excluded fields parameter allows filtering out specific fields from
     * the returned data. The map structure is: key is the entity/table/keyspace name,
     * value is a set of field names to exclude from that entity.
     * </p>
     *
     * @param excludedFields a map where keys are entity identifiers (table names,
     *                       keyspace names, etc.) and values are sets of field names
     *                       to exclude from the returned data. May be {@code null} or empty.
     * @return a map representing the current state of all data, where keys are
     * composite identifiers and values are the data objects. The map should
     * preserve insertion order if possible.
     * @throws Exception if an error occurs while retrieving the data
     */
    Map<String, Object> getCurrentValue(Map<String, Set<String>> excludedFields) throws Exception;

    /**
     * Notifies this storage about detected differences in the data state.
     * <p>
     * This method is called by the testing framework when differences are detected
     * between the expected and actual data state. Implementations can override this
     * method to track changes, update internal state, or perform cleanup operations.
     * </p>
     * <p>
     * The default implementation does nothing. Implementations that need to track
     * differences should override this method.
     * </p>
     *
     * @param diff a map representing the differences detected, or {@code null} if
     *             no differences were found or when resetting the difference state
     */
    default void setDiff(Map<String, Object> diff) {
    }
}
