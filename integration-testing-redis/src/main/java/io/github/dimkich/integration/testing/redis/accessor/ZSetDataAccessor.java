package io.github.dimkich.integration.testing.redis.accessor;

import io.github.dimkich.integration.testing.redis.client.RedisPipeline;
import io.github.dimkich.integration.testing.redis.client.ScoredMember;
import io.github.dimkich.integration.testing.redis.model.RedisZSet;
import io.github.dimkich.integration.testing.redis.serde.RedisDataSchema;

import java.util.List;

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
     * each {@code RedisZSetEntry}.
     *
     * @param key      serialized Redis key
     * @param data     sorted-set members and scores to store
     * @param pipeline pipeline used for commands
     * @param schema   codec for member values
     */
    @Override
    public void store(byte[] key, RedisZSet data, RedisPipeline pipeline, RedisDataSchema schema) {
        pipeline.del(key);
        if (data.isEmpty()) {
            return;
        }

        List<ScoredMember> members = data.stream()
                .map(entry -> new ScoredMember(
                        schema.getValueCodec().serialize(entry.getMember()),
                        entry.getScore().doubleValue()))
                .toList();

        pipeline.zAdd(key, members);
    }
}
