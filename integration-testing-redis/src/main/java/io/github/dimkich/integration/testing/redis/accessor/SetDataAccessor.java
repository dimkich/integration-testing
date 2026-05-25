package io.github.dimkich.integration.testing.redis.accessor;

import io.github.dimkich.integration.testing.redis.model.RedisSet;
import io.github.dimkich.integration.testing.redis.schema.RedisDataSchema;
import org.springframework.data.redis.connection.RedisConnection;

/**
 * {@link RedisDataAccessor} that writes {@link RedisSet} values to Redis.
 * <p>
 * Replaces the target key atomically: deletes the key, then populates it with
 * {@code SADD} using the value codec from {@link RedisDataSchema}.
 */
public class SetDataAccessor implements RedisDataAccessor<RedisSet> {

    /** {@inheritDoc} */
    @Override
    public Class<RedisSet> getSupportedClass() {
        return RedisSet.class;
    }

    /**
     * Writes a set to Redis, replacing any existing value at {@code key}.
     * <p>
     * Empty sets only delete the key. Non-empty sets are stored with {@code SADD};
     * member order in the model is not preserved in Redis.
     *
     * @param key    serialized Redis key
     * @param data   set members to store
     * @param conn   connection used for commands
     * @param schema codec for set member values
     */
    @Override
    public void store(byte[] key, RedisSet data, RedisConnection conn, RedisDataSchema schema) {
        conn.keyCommands().del(key);
        if (data.isEmpty()) {
            return;
        }
        byte[][] raw = data.stream()
                .map(schema.getValueCodec()::serialize)
                .toArray(byte[][]::new);
        if (conn.setCommands() == null) {
            conn.sAdd(key, raw);
        } else {
            conn.setCommands().sAdd(key, raw);
        }
    }
}
