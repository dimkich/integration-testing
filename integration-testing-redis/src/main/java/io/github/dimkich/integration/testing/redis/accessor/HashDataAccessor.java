package io.github.dimkich.integration.testing.redis.accessor;

import io.github.dimkich.integration.testing.date.time.DateTimeService;
import io.github.dimkich.integration.testing.redis.model.RedisEntry;
import io.github.dimkich.integration.testing.redis.model.RedisHash;
import io.github.dimkich.integration.testing.redis.schema.RedisDataSchema;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.ReturnType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link RedisDataAccessor} that writes {@link RedisHash} values to Redis.
 * <p>
 * Replaces the target key atomically: deletes the key, then populates it with
 * {@code HMSET} using the connection's hash key and hash value codecs from
 * {@link RedisDataSchema}. Fields with
 * {@link RedisEntry} expiration metadata receive per-field TTL via {@code HPEXPIREAT} in a Lua script.
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
     * Empty hashes only delete the key. Non-empty hashes are stored with {@code HMSET};
     * fields that define {@code expireAt} or {@code ttl} on their entry are given
     * millisecond-precision expiration via {@code HPEXPIREAT}.
     *
     * @param key    serialized Redis key
     * @param data   hash fields and values to store
     * @param conn   connection used for commands
     * @param schema codecs for hash field keys and values
     */
    @Override
    public void store(byte[] key, RedisHash data, RedisConnection conn, RedisDataSchema schema) {
        conn.keyCommands().del(key);
        if (data.isEmpty()) return;

        Map<byte[], byte[]> rawValues = new LinkedHashMap<>();
        List<byte[]> ttlArgs = new ArrayList<>();

        data.forEach((fieldKey, entry) -> {
            byte[] rawField = schema.getHashKeyCodec().serialize(fieldKey);
            rawValues.put(rawField, schema.getHashValueCodec().serialize(entry.getData()));

            if (entry.getExpireAt() != null || entry.getTtl() != null) {
                Instant expireInstant = entry.getExpireInstant(dateTimeService.getDateTime());
                if (expireInstant != null) {
                    ttlArgs.add(String.valueOf(expireInstant.toEpochMilli()).getBytes());
                    ttlArgs.add(rawField);
                }
            }
        });

        conn.hashCommands().hMSet(key, rawValues);

        if (!ttlArgs.isEmpty()) {
            byte[] script = ("for i=1, #ARGV, 2 do " +
                    "redis.call('HPEXPIREAT', KEYS[1], ARGV[i], 'FIELDS', '1', ARGV[i+1]) " +
                    "end").getBytes();
            byte[][] args = ttlArgs.toArray(new byte[0][]);
            conn.scriptingCommands().eval(script, ReturnType.STATUS, 1, concat(key, args));
        }
    }

    /**
     * Prepends {@code first} as the sole element of {@code KEYS[1]} for {@code EVAL}.
     *
     * @param first the Redis key
     * @param rest  script arguments (expire-at millis and field names, alternating)
     * @return array whose first element is {@code first}, followed by {@code rest}
     */
    private byte[][] concat(byte[] first, byte[][] rest) {
        byte[][] total = new byte[rest.length + 1][];
        total[0] = first;
        System.arraycopy(rest, 0, total, 1, rest.length);
        return total;
    }
}