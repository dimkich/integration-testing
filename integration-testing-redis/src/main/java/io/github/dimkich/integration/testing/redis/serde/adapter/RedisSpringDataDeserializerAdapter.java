package io.github.dimkich.integration.testing.redis.serde.adapter;

import io.github.dimkich.integration.testing.redis.serde.RedisComponentRole;
import io.github.dimkich.integration.testing.serde.StandardSerdeProperties;
import io.github.dimkich.integration.testing.serde.TestSerdeAdapter;
import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import lombok.Getter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.lang.Nullable;

import java.nio.ByteBuffer;

/**
 * Adapts a Spring Data {@link RedisSerializer} bean to a core deserializer converter. The binary
 * value is copied to {@code byte[]} only at the native deserializer boundary.
 * <p>
 * Spring Data is an optional dependency of the module: the bean is registered only when Spring
 * Data Redis is present on the application classpath.
 *
 * @see RedisSpringDataSerializerAdapter
 */
@Getter
@ConditionalOnClass(name = "org.springframework.data.redis.serializer.RedisSerializer")
@SuppressWarnings("rawtypes")
public class RedisSpringDataDeserializerAdapter
        implements TestSerdeAdapter<RedisSerializer, ByteBuffer, Object, TestSerdeContext, RedisComponentRole,
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
            Class<Object> outputClass, Class<TestSerdeContext> contextClass, @Nullable RedisComponentRole role) {
        return TestSerdeConverter.of(inputClass, outputClass, contextClass,
                (input, context) -> input == null ? null
                        : source.deserialize(TestSerdeConverter.toByteArray(input)));
    }
}
