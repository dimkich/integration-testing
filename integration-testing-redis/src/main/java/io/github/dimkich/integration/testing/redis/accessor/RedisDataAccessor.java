package io.github.dimkich.integration.testing.redis.accessor;

import io.github.dimkich.integration.testing.redis.model.RedisEntry;
import io.github.dimkich.integration.testing.redis.model.RedisValue;
import io.github.dimkich.integration.testing.redis.schema.RedisDataSchema;
import org.springframework.data.redis.connection.RedisConnection;

/**
 * Strategy for persisting a typed {@link RedisValue} to Redis through a
 * {@link RedisConnection}.
 * <p>
 * Implementations are Spring beans collected by {@link RedisAccessorCoordinator}, which
 * indexes them by {@link #getSupportedClass()} and dispatches each value to the matching
 * accessor. Plain (non-{@link RedisValue}) objects are handled separately by
 * {@link StringDataAccessor}.
 *
 * @param <D> the {@link RedisValue} subtype this accessor supports
 */
public interface RedisDataAccessor<D extends RedisValue> {

    /**
     * Returns the {@link RedisValue} class this accessor handles.
     * <p>
     * Used at startup to build the coordinator's type-to-accessor map; must match the
     * runtime class of values passed to {@link #store(byte[], RedisValue, RedisConnection, RedisDataSchema)}.
     */
    Class<D> getSupportedClass();

    /**
     * Writes {@code data} to Redis at {@code keyRaw}, using codecs from {@code schema}.
     * <p>
     * Implementations typically replace the key atomically (delete then repopulate). Entry-level
     * TTL on {@link RedisEntry} wrappers is applied by {@link RedisAccessorCoordinator}, not by
     * individual accessors.
     *
     * @param keyRaw encoded Redis key
     * @param data   typed test value to persist
     * @param conn   connection used for writes
     * @param schema codecs and metadata for encoding
     */
    void store(byte[] keyRaw, D data, RedisConnection conn, RedisDataSchema schema);
}
