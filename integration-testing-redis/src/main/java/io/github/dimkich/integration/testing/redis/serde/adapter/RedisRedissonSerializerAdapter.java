package io.github.dimkich.integration.testing.redis.serde.adapter;

import io.github.dimkich.integration.testing.redis.serde.RedisComponentRole;
import io.github.dimkich.integration.testing.serde.StandardSerdeProperties;
import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import lombok.Getter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.lang.Nullable;

import java.nio.ByteBuffer;

/**
 * Adapts a Redisson source to a core serializer converter backed by the encoder of the
 * requested component slot of a Redisson {@code Codec}. The native encoder returns
 * {@code byte[]}, which is wrapped into a read-ready {@link ByteBuffer}.
 *
 * @see AbstractRedissonAdapter
 */
@Getter
@ConditionalOnClass(name = "org.redisson.client.codec.Codec")
public class RedisRedissonSerializerAdapter extends AbstractRedissonAdapter<Object, ByteBuffer> {

    private final Class<Object> inputClass = Object.class;

    private final Class<ByteBuffer> outputClass = ByteBuffer.class;

    @Override
    @Nullable
    public TestSerdeConverter<Object, ByteBuffer, TestSerdeContext> adapt(
            Object source, StandardSerdeProperties properties, Class<Object> inputClass,
            Class<ByteBuffer> outputClass, Class<TestSerdeContext> contextClass,
            @Nullable RedisComponentRole role) {
        RedissonSlotCodec codec = resolveCodec(source, role);
        if (codec == null) {
            return null;
        }
        return TestSerdeConverter.of(inputClass, outputClass, contextClass,
                (input, context) -> TestSerdeConverter.wrap(codec.serialize(input)));
    }
}
