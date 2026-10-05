package io.github.dimkich.integration.testing.redis.serde.adapter;

import io.github.dimkich.integration.testing.redis.serde.RedisComponentRole;
import io.github.dimkich.integration.testing.serde.StandardSerdeProperties;
import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import lombok.Getter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.lang.Nullable;

/**
 * Adapts a Redisson source to a core deserializer converter backed by the decoder of the
 * requested component slot of a Redisson {@code Codec}.
 *
 * @see AbstractRedissonAdapter
 */
@Getter
@ConditionalOnClass(name = "org.redisson.client.codec.Codec")
public class RedisRedissonDeserializerAdapter extends AbstractRedissonAdapter<byte[], Object> {

    private final Class<byte[]> inputClass = byte[].class;

    private final Class<Object> outputClass = Object.class;

    @Override
    @Nullable
    public TestSerdeConverter<byte[], Object, TestSerdeContext> adapt(
            Object source, StandardSerdeProperties properties, Class<byte[]> inputClass,
            Class<Object> outputClass, Class<TestSerdeContext> contextClass,
            @Nullable RedisComponentRole role) {
        RedissonSlotCodec codec = resolveCodec(source, role);
        if (codec == null) {
            return null;
        }
        return TestSerdeConverter.of(inputClass, outputClass, contextClass,
                (input, context) -> codec.deserialize(input));
    }
}
