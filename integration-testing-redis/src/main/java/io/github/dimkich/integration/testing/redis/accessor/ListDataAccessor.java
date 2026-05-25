package io.github.dimkich.integration.testing.redis.accessor;

import io.github.dimkich.integration.testing.redis.model.RedisList;
import io.github.dimkich.integration.testing.redis.schema.RedisDataSchema;
import org.springframework.data.redis.connection.RedisConnection;

/**
 * {@link RedisDataAccessor} that writes {@link RedisList} values to Redis.
 * <p>
 * Replaces the target key atomically: deletes the key, then populates it with
 * {@code RPUSH} using the value codec from {@link RedisDataSchema}.
 */
public class ListDataAccessor implements RedisDataAccessor<RedisList> {

    /** {@inheritDoc} */
    @Override
    public Class<RedisList> getSupportedClass() {
        return RedisList.class;
    }

    /**
     * Writes a list to Redis, replacing any existing value at {@code key}.
     * <p>
     * Empty lists only delete the key. Non-empty lists are stored with {@code RPUSH},
     * preserving element order from first to last.
     *
     * @param key    serialized Redis key
     * @param data   list elements to store
     * @param conn   connection used for commands
     * @param schema codec for list element values
     */
    @Override
    public void store(byte[] key, RedisList data, RedisConnection conn, RedisDataSchema schema) {
        conn.keyCommands().del(key);
        if (data.isEmpty()) {
            return;
        }
        byte[][] raw = data.stream()
                .map(schema.getValueCodec()::serialize)
                .toArray(byte[][]::new);
        conn.listCommands().rPush(key, raw);
    }
}

