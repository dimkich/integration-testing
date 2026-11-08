package io.github.dimkich.integration.testing.redis.accessor;

import io.github.dimkich.integration.testing.date.time.DateTimeService;
import io.github.dimkich.integration.testing.redis.client.RedisPipeline;
import io.github.dimkich.integration.testing.redis.model.RedisEntry;
import io.github.dimkich.integration.testing.redis.model.RedisValue;
import io.github.dimkich.integration.testing.redis.serde.RedisDataSchema;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Routes test data values to the appropriate {@link RedisDataAccessor} and writes them through a
 * {@link RedisPipeline}.
 * <p>
 * At startup, all injected {@link RedisDataAccessor} beans are indexed by
 * {@link RedisDataAccessor#getSupportedClass()}. Values that are not a {@link RedisValue} subtype
 * are handled by {@link StringDataAccessor}. {@link RedisEntry} wrappers are unwrapped before
 * dispatch; optional entry-level TTL is applied with {@code PEXPIREAT} after the value is stored.
 */
@RequiredArgsConstructor
public class RedisAccessorCoordinator {
    private final List<RedisDataAccessor<? extends RedisValue>> accessors;
    private final StringDataAccessor stringDataAccessor;
    @Setter(onMethod_ = {@Autowired, @Lazy})
    private DateTimeService dateTimeService;
    private final Map<Class<?>, RedisDataAccessor<RedisValue>> accessorsMap = new HashMap<>();

    /** Registers each injected accessor by its {@link RedisDataAccessor#getSupportedClass()}. */
    @PostConstruct
    @SuppressWarnings("unchecked")
    void init() {
        for (RedisDataAccessor<? extends RedisValue> accessor : accessors) {
            accessorsMap.put(accessor.getSupportedClass(), (RedisDataAccessor<RedisValue>) accessor);
        }
    }

    /**
     * Stores a key-value pair in Redis using the accessor that matches the value type.
     *
     * @param keyRaw   encoded Redis key
     * @param value    {@link RedisEntry}, a {@link RedisValue}, or a plain object for string storage
     * @param pipeline pipeline used for writes
     * @param schema   codecs and metadata for key/value encoding
     */
    public void store(byte[] keyRaw, Object value, RedisPipeline pipeline, RedisDataSchema schema) {
        if (value instanceof RedisEntry entry) {
            findAndStore(keyRaw, entry.getData(), pipeline, schema);
            Instant expireInstant = entry.getExpireInstant(dateTimeService.getDateTime());
            if (expireInstant != null) {
                pipeline.pExpireAt(keyRaw, expireInstant.toEpochMilli());
            }
        } else {
            findAndStore(keyRaw, value, pipeline, schema);
        }
    }

    /**
     * Dispatches {@code data} to a typed accessor or {@link StringDataAccessor}; no-op when
     * {@code data} is {@code null}.
     */
    private void findAndStore(byte[] key, Object data, RedisPipeline pipeline, RedisDataSchema schema) {
        if (data == null) {
            return;
        }
        if (data instanceof RedisValue redisValue) {
            RedisDataAccessor<RedisValue> accessor = accessorsMap.get(data.getClass());
            if (accessor == null) {
                throw new RuntimeException("No accessor registered for " + data.getClass());
            }
            accessor.store(key, redisValue, pipeline, schema);
        } else {
            stringDataAccessor.store(key, data, pipeline, schema);
        }
    }
}
