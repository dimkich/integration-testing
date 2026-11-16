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
 * Adapts a Spring Data {@link RedisSerializer} bean to a core serializer converter. The native
 * serializer returns {@code byte[]}, which is wrapped into a read-ready {@link ByteBuffer}.
 * <p>
 * Spring Data is an optional dependency of the module: the bean is registered only when Spring
 * Data Redis is present on the application classpath.
 *
 * @see RedisSpringDataDeserializerAdapter
 */
@Getter
@ConditionalOnClass(name = "org.springframework.data.redis.serializer.RedisSerializer")
@SuppressWarnings("rawtypes")
public class RedisSpringDataSerializerAdapter
        implements TestSerdeAdapter<RedisSerializer, Object, ByteBuffer, TestSerdeContext, RedisComponentRole,
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
            Class<ByteBuffer> outputClass, Class<TestSerdeContext> contextClass, @Nullable RedisComponentRole role) {
        return TestSerdeConverter.of(inputClass, outputClass, contextClass,
                (input, context) -> input == null ? null : TestSerdeConverter.wrap(source.serialize(input)));
    }
}
