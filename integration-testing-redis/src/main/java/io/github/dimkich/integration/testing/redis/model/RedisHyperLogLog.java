package io.github.dimkich.integration.testing.redis.model;

import java.util.TreeSet;

/**
 * In-memory representation of a Redis HyperLogLog.
 * <p>
 * Approximate cardinality is tracked as a {@link TreeSet} of observed elements ordered
 * by {@link RedisDataComparator}, mirroring the test store's simplified HLL model.
 */
public class RedisHyperLogLog extends TreeSet<Object> implements RedisValue {

    /** Creates an empty HyperLogLog with {@link RedisDataComparator}-ordered elements. */
    public RedisHyperLogLog() {
        super(new RedisDataComparator());
    }
}