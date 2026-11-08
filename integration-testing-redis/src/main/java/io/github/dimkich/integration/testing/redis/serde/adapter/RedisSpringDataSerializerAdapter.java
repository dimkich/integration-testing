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
 * Adapts a Spring Data {@code RedisSerializer} bean to a core serializer converter. The native
 * serializer returns {@code byte[]}, which is wrapped into a read-ready {@link ByteBuffer}.
 * <p>
 * Spring Data is not a compile-time dependency of the module; the serializer class is loaded by
 * name and used reflectively. The bean is only registered when Spring Data Redis is present.
 *
 * @see RedisSpringDataDeserializerAdapter
 */
@Getter
@ConditionalOnClass(name = "org.springframework.data.redis.serializer.RedisSerializer")
public class RedisSpringDataSerializerAdapter extends AbstractSpringDataAdapter<Object, ByteBuffer> {

    private final Class<Object> inputClass = Object.class;

    private final Class<ByteBuffer> outputClass = ByteBuffer.class;

    @Override
    @Nullable
    public TestSerdeConverter<Object, ByteBuffer, TestSerdeContext> adapt(
            Object source, StandardSerdeProperties properties, Class<Object> inputClass,
            Class<ByteBuffer> outputClass, Class<TestSerdeContext> contextClass, @Nullable RedisComponentRole role) {
        return TestSerdeConverter.of(inputClass, outputClass, contextClass,
                (input, context) -> {
                    if (input == null) {
                        return null;
                    }
                    byte[] bytes = serialize(source, input);
                    return bytes == null ? null : TestSerdeConverter.wrap(bytes);
                });
    }
}
