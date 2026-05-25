package io.github.dimkich.integration.testing.redis.accessor;

import io.github.dimkich.integration.testing.redis.model.RedisZSet;
import io.github.dimkich.integration.testing.redis.model.RedisZSetEntry;
import io.github.dimkich.integration.testing.redis.schema.RedisDataSchema;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.zset.DefaultTuple;
import org.springframework.data.redis.connection.zset.Tuple;

import java.util.Set;

/**
 * {@link RedisDataAccessor} that writes {@link RedisZSet} values to Redis.
 * <p>
 * Replaces the target key atomically: deletes the key, then populates it with
 * {@code ZADD} using the value codec from {@link RedisDataSchema} for members and
 * each entry's score from the model.
 */
public class ZSetDataAccessor implements RedisDataAccessor<RedisZSet> {

    /** {@inheritDoc} */
    @Override
    public Class<RedisZSet> getSupportedClass() {
        return RedisZSet.class;
    }

    /**
     * Writes a sorted set to Redis, replacing any existing value at {@code key}.
     * <p>
     * Empty sets only delete the key. Non-empty sets are stored with {@code ZADD};
     * members are serialized with the schema value codec and scores are taken from
     * each {@link RedisZSetEntry}.
     *
     * @param key    serialized Redis key
     * @param data   sorted-set members and scores to store
     * @param conn   connection used for commands
     * @param schema codec for member values
     */
    @Override
    public void store(byte[] key, RedisZSet data, RedisConnection conn, RedisDataSchema schema) {
        conn.keyCommands().del(key);
        if (data.isEmpty()) {
            return;
        }

        Set<Tuple> tuples = data.stream()
                .map(e -> new DefaultTuple(
                        schema.getValueCodec().serialize(e.getMember()),
                        e.getScore().doubleValue()
                ))
                .collect(java.util.stream.Collectors.toSet());

        conn.zSetCommands().zAdd(key, tuples);
    }
}