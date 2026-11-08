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
 * Adapts a Spring Data {@code RedisSerializer} bean to a core deserializer converter. The binary
 * value is copied to {@code byte[]} only at the native deserializer boundary.
 * <p>
 * Spring Data is not a compile-time dependency of the module; the serializer class is loaded by
 * name and used reflectively. The bean is only registered when Spring Data Redis is present.
 *
 * @see RedisSpringDataSerializerAdapter
 */
@Getter
@ConditionalOnClass(name = "org.springframework.data.redis.serializer.RedisSerializer")
public class RedisSpringDataDeserializerAdapter extends AbstractSpringDataAdapter<ByteBuffer, Object> {

    private final Class<ByteBuffer> inputClass = ByteBuffer.class;

    private final Class<Object> outputClass = Object.class;

    @Override
    @Nullable
    public TestSerdeConverter<ByteBuffer, Object, TestSerdeContext> adapt(
            Object source, StandardSerdeProperties properties, Class<ByteBuffer> inputClass,
            Class<Object> outputClass, Class<TestSerdeContext> contextClass, @Nullable RedisComponentRole role) {
        return TestSerdeConverter.of(inputClass, outputClass, contextClass,
                (input, context) -> input == null ? null
                        : deserialize(source, TestSerdeConverter.toByteArray(input)));
    }
}
