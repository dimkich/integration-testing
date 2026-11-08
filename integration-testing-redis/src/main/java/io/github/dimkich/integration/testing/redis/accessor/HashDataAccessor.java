package io.github.dimkich.integration.testing.redis.accessor;

import io.github.dimkich.integration.testing.date.time.DateTimeService;
import io.github.dimkich.integration.testing.redis.client.RedisPipeline;
import io.github.dimkich.integration.testing.redis.model.RedisEntry;
import io.github.dimkich.integration.testing.redis.model.RedisHash;
import io.github.dimkich.integration.testing.redis.serde.RedisDataSchema;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link RedisDataAccessor} that writes {@link RedisHash} values to Redis.
 * <p>
 * Replaces the target key atomically: deletes the key, then populates it with {@code HSET}
 * using the connection's hash key and hash value codecs from {@link RedisDataSchema}. Fields
 * with {@link RedisEntry} expiration metadata receive per-field TTL via {@code HPEXPIREAT};
 * all such updates of a pipeline are collapsed into one Lua {@code EVAL} at flush time.
 */
public class HashDataAccessor implements RedisDataAccessor<RedisHash> {
    @Setter(onMethod_ = {@Autowired, @Lazy})
    private DateTimeService dateTimeService;

    /** {@inheritDoc} */
    @Override
    public Class<RedisHash> getSupportedClass() {
        return RedisHash.class;
    }

    /**
     * Writes a hash to Redis, replacing any existing value at {@code key}.
     * <p>
     * Empty hashes only delete the key. Non-empty hashes are stored with {@code HSET};
     * fields that define {@code expireAt} or {@code ttl} on their entry are given
     * millisecond-precision expiration via {@code HPEXPIREAT}.
     *
     * @param key      serialized Redis key
     * @param data     hash fields and values to store
     * @param pipeline pipeline used for commands
     * @param schema   codecs for hash field keys and values
     */
    @Override
    public void store(byte[] key, RedisHash data, RedisPipeline pipeline, RedisDataSchema schema) {
        pipeline.del(key);
        if (data.isEmpty()) {
            return;
        }

        Map<byte[], byte[]> rawValues = new LinkedHashMap<>();
        List<byte[]> expiringFields = new ArrayList<>();
        List<Long> expirations = new ArrayList<>();

        data.forEach((fieldKey, entry) -> {
            byte[] rawField = schema.getHashKeyCodec().serialize(fieldKey);
            rawValues.put(rawField, schema.getHashValueCodec().serialize(entry.getData()));

            if (entry.getExpireAt() != null || entry.getTtl() != null) {
                Instant expireInstant = entry.getExpireInstant(dateTimeService.getDateTime());
                if (expireInstant != null) {
                    expiringFields.add(rawField);
                    expirations.add(expireInstant.toEpochMilli());
                }
            }
        });

        pipeline.hSet(key, rawValues);

        for (int i = 0; i < expiringFields.size(); i++) {
            pipeline.hExpireAt(key, expiringFields.get(i), expirations.get(i));
        }
    }
}
