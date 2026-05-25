package io.github.dimkich.integration.testing.redis.model;

import io.github.dimkich.integration.testing.redis.schema.RedisDataSchema;

import java.util.ArrayList;

/**
 * In-memory representation of a Redis list.
 * <p>
 * Elements preserve insertion order. Values deserialized from RDB replication are decoded
 * with the schema's {@link RedisDataSchema#getValueCodec() value codec}.
 */
public class RedisList extends ArrayList<Object> implements RedisValue {

    /**
     * Appends a deserialized element from an RDB list entry.
     *
     * @param schema codecs for list elements
     * @param item   raw element bytes
     */
    public void add(RedisDataSchema schema, byte[] item) {
        add(schema.getValueCodec().deserialize(item));
    }
}
