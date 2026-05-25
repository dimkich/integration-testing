package io.github.dimkich.integration.testing.redis.codec;

/**
 * Adapts Redis client-specific beans (e.g. Spring Data {@code RedisOperations}, Redisson {@code Codec})
 * into a unified {@link RedisDataCodec}.
 * <p>
 * Used by {@link io.github.dimkich.integration.testing.redis.registry.RedisObjectFactory} when resolving
 * codec references from configuration: if the resolved bean is not already a {@link RedisDataCodec},
 * adapters are tried in order until one returns a non-null codec.
 * <p>
 * Implementations include {@link SpringDataCodecAdapter} and {@link RedissonCodecAdapter}.
 */
public interface RedisDataCodecAdapter {

    /**
     * Attempts to create a {@link RedisDataCodec} from the given bean.
     *
     * @param bean the bean to adapt (e.g. {@code RedisOperations}, {@code Codec}, or similar)
     * @return a {@link RedisDataCodec} if this adapter supports the bean type, otherwise {@code null}
     */
    RedisDataCodec tryCreateCodec(Object bean);
}
