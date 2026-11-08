package io.github.dimkich.integration.testing.redis.accessor;

import io.github.dimkich.integration.testing.redis.client.RedisPipeline;
import io.github.dimkich.integration.testing.redis.model.RedisStream;
import io.github.dimkich.integration.testing.redis.model.RedisStreamEntry;
import io.github.dimkich.integration.testing.redis.serde.RedisDataSchema;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link RedisDataAccessor} that writes {@link RedisStream} values to Redis.
 * <p>
 * Replaces the target key atomically: deletes the key, then appends each
 * {@link RedisStreamEntry} with {@code XADD}, using hash field and value codecs
 * from {@link RedisDataSchema}.
 */
public class StreamDataAccessor implements RedisDataAccessor<RedisStream> {

    /** {@inheritDoc} */
    @Override
    public Class<RedisStream> getSupportedClass() {
        return RedisStream.class;
    }

    /**
     * Writes a stream to Redis, replacing any existing value at {@code key}.
     * <p>
     * Empty streams only delete the key. Non-empty streams are stored entry by entry
     * with {@code XADD}; each entry keeps its model id and field map order is preserved
     * via a {@link LinkedHashMap} before serialization.
     *
     * @param key      serialized Redis key
     * @param data     stream entries to store
     * @param pipeline pipeline used for commands
     * @param schema   codecs for stream field names and values
     */
    @Override
    public void store(byte[] key, RedisStream data, RedisPipeline pipeline, RedisDataSchema schema) {
        pipeline.del(key);
        for (RedisStreamEntry record : data) {
            Map<byte[], byte[]> fields = new LinkedHashMap<>();
            record.getFields().forEach((field, value) -> fields.put(
                    schema.getHashKeyCodec().serialize(field),
                    schema.getHashValueCodec().serialize(value)));

            pipeline.xAdd(key, record.getId().getBytes(StandardCharsets.UTF_8), fields);
        }
    }
}
