package io.github.dimkich.integration.testing.serde.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dimkich.integration.testing.serde.*;
import io.github.dimkich.integration.testing.serde.impl.JacksonSerializer;
import io.github.dimkich.integration.testing.serde.properties.ProviderProperties;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;

import java.nio.ByteBuffer;

/**
 * Base of Jackson-based serializer providers: resolves the mapper bean (by
 * {@code objectMapperRef} or as the single bean of {@link #getMapperClass()}) and creates a
 * {@link JacksonSerializer}.
 *
 * @see JacksonDeserializerFactory
 */
@Getter
@RequiredArgsConstructor
public abstract class JacksonSerializerFactory
        implements TestSerdeProviderFactory<Object, ByteBuffer, TestSerdeContext, ComponentRole,
        ProviderProperties> {

    @Getter(AccessLevel.NONE)
    protected final BeanResolver beanResolver;

    private final String name;

    private final Class<Object> inputClass = Object.class;

    private final Class<ByteBuffer> outputClass = ByteBuffer.class;

    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    private final Class<ProviderProperties> propertiesClass = ProviderProperties.class;

    @Override
    public TestSerdeConverter<Object, ByteBuffer, TestSerdeContext> create(
            ProviderProperties config, Class<Object> inputClass, Class<ByteBuffer> outputClass,
            Class<TestSerdeContext> contextClass, @Nullable ComponentRole role) {
        return new JacksonSerializer(beanResolver.resolve(config.getObjectMapperRef(), getMapperClass()));
    }

    protected Class<? extends ObjectMapper> getMapperClass() {
        return ObjectMapper.class;
    }
}
