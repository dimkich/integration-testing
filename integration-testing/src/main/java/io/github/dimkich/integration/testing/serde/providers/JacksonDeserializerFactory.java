package io.github.dimkich.integration.testing.serde.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dimkich.integration.testing.serde.*;
import io.github.dimkich.integration.testing.serde.impl.JacksonDeserializer;
import io.github.dimkich.integration.testing.serde.properties.ProviderProperties;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;

import java.lang.reflect.Type;

/**
 * Base of Jackson-based deserializer providers: resolves the mapper bean (by
 * {@code objectMapperRef} or as the single bean of {@link #getMapperClass()}) and creates a
 * {@link JacksonDeserializer} targeting {@code targetClass} when configured, otherwise the
 * requested output class.
 *
 * @see JacksonSerializerFactory
 */
@Getter
@RequiredArgsConstructor
public abstract class JacksonDeserializerFactory
        implements TestSerdeProviderFactory<byte[], Object, TestSerdeContext, ComponentRole, ProviderProperties> {

    @Getter(AccessLevel.NONE)
    protected final BeanResolver beanResolver;

    private final String name;

    private final Class<byte[]> inputClass = byte[].class;

    private final Class<Object> outputClass = Object.class;

    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    private final Class<ProviderProperties> propertiesClass = ProviderProperties.class;

    @Override
    public TestSerdeConverter<byte[], Object, TestSerdeContext> create(ProviderProperties config, Class<byte[]> inputClass,
                                                                       Class<Object> outputClass,
                                                                       Class<TestSerdeContext> contextClass,
                                                                       @Nullable ComponentRole role) {
        Type targetType = config.getTargetClass() != null ? config.getTargetClass() : outputClass;
        return new JacksonDeserializer(beanResolver.resolve(config.getObjectMapperRef(), getMapperClass()), targetType);
    }

    protected Class<? extends ObjectMapper> getMapperClass() {
        return ObjectMapper.class;
    }
}
