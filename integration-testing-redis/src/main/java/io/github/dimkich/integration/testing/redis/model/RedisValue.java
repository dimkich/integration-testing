package io.github.dimkich.integration.testing.redis.model;

import java.util.Map;
import java.util.stream.Stream;

/**
 * Marker for typed in-memory Redis data structures ({@link RedisHash}, {@link RedisList},
 * {@link RedisSet}, {@link RedisZSet}, {@link RedisStream}, {@link RedisHyperLogLog}).
 * <p>
 * Used by {@link io.github.dimkich.integration.testing.redis.accessor.RedisDataAccessor}
 * to dispatch persistence and by {@link RedisEntry} to detect empty composite values.
 */
public interface RedisValue {

    /**
     * Returns {@code true} when this structure contains no elements.
     *
     * @return whether the value is empty
     */
    boolean isEmpty();

    /**
     * Returns a stream of nested {@link RedisEntry} wrappers for structures that support
     * per-element TTL (for example {@link RedisHash} fields).
     * <p>
     * Default implementation returns an empty stream.
     *
     * @return nested entries, possibly empty
     */
    default Stream<Map.Entry<Object, RedisEntry>> nestedEntries() {
        return Stream.of();
    }
}
