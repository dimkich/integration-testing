package io.github.dimkich.integration.testing.redis.serde.adapter;

import io.github.dimkich.integration.testing.serde.*;
import lombok.Getter;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.lang.Nullable;

import java.nio.ByteBuffer;

/**
 * Adapts a single-slot Spring Data {@link RedisSerializer} bean to a core serializer
 * converter (component level). The native serializer returns {@code byte[]}, which is wrapped
 * into a read-ready {@link ByteBuffer}.
 *
 * @see RedisSpringDataDeserializerAdapter
 */
@Getter
@SuppressWarnings("rawtypes")
public class RedisSpringDataSerializerAdapter
        implements TestSerdeAdapter<RedisSerializer, Object, ByteBuffer, TestSerdeContext, ComponentRole,
        StandardSerdeProperties> {

    private final Class<RedisSerializer> sourceClass = RedisSerializer.class;

    private final Class<Object> inputClass = Object.class;

    private final Class<ByteBuffer> outputClass = ByteBuffer.class;

    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    private final Class<StandardSerdeProperties> propertiesClass = StandardSerdeProperties.class;

    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public TestSerdeConverter<Object, ByteBuffer, TestSerdeContext> adapt(
            RedisSerializer source, StandardSerdeProperties properties, Class<Object> inputClass,
            Class<ByteBuffer> outputClass, Class<TestSerdeContext> contextClass, @Nullable ComponentRole role) {
        return TestSerdeConverter.of(inputClass, outputClass, contextClass,
                (input, context) -> input == null ? null : TestSerdeConverter.wrap(source.serialize(input)));
    }
}
