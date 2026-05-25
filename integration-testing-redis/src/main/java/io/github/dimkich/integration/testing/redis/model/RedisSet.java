package io.github.dimkich.integration.testing.redis.model;

import io.github.dimkich.integration.testing.redis.schema.RedisDataSchema;

import java.util.TreeSet;

/**
 * In-memory representation of a Redis set.
 * <p>
 * Members are stored in a {@link TreeSet} ordered by {@link RedisDataComparator}.
 * Values deserialized from RDB replication are decoded with the schema's
 * {@link RedisDataSchema#getValueCodec() value codec}.
 */
public class RedisSet extends TreeSet<Object> implements RedisValue {

    /** Creates an empty set with {@link RedisDataComparator}-ordered members. */
    public RedisSet() {
        super(new RedisDataComparator());
    }

    /**
     * Inserts a deserialized member from an RDB set entry.
     *
     * @param schema codecs for set members
     * @param item   raw member bytes
     */
    public void add(RedisDataSchema schema, byte[] item) {
        add(schema.getValueCodec().deserialize(item));
    }
}
