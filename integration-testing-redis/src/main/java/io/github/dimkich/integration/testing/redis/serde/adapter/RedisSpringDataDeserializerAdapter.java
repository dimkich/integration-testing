package io.github.dimkich.integration.testing.redis.serde.adapter;

import io.github.dimkich.integration.testing.serde.*;
import lombok.Getter;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.lang.Nullable;

import java.nio.ByteBuffer;

/**
 * Adapts a single-slot Spring Data {@link RedisSerializer} bean to a core deserializer
 * converter (component level). The binary value is copied to {@code byte[]} only at the native
 * deserializer boundary.
 *
 * @see RedisSpringDataSerializerAdapter
 */
@Getter
@SuppressWarnings("rawtypes")
public class RedisSpringDataDeserializerAdapter
        implements TestSerdeAdapter<RedisSerializer, ByteBuffer, Object, TestSerdeContext, ComponentRole,
        StandardSerdeProperties> {

    private final Class<RedisSerializer> sourceClass = RedisSerializer.class;

    private final Class<ByteBuffer> inputClass = ByteBuffer.class;

    private final Class<Object> outputClass = Object.class;

    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    private final Class<StandardSerdeProperties> propertiesClass = StandardSerdeProperties.class;

    @Override
    @Nullable
    public TestSerdeConverter<ByteBuffer, Object, TestSerdeContext> adapt(
            RedisSerializer source, StandardSerdeProperties properties, Class<ByteBuffer> inputClass,
            Class<Object> outputClass, Class<TestSerdeContext> contextClass, @Nullable ComponentRole role) {
        return TestSerdeConverter.of(inputClass, outputClass, contextClass,
                (input, context) -> input == null ? null
                        : source.deserialize(TestSerdeConverter.toByteArray(input)));
    }
}
