package io.github.dimkich.integration.testing.serde.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dimkich.integration.testing.serde.BeanResolver;
import io.github.dimkich.integration.testing.serde.SerdeProperties;
import io.github.dimkich.integration.testing.serde.TestSerdeProvider;
import io.github.dimkich.integration.testing.serde.impl.JacksonDeserializer;
import io.github.dimkich.integration.testing.serde.impl.JacksonSerializer;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

/**
 * Base provider for all Jackson-based formats. Builds a
 * {@link JacksonSerializer}/{@link JacksonDeserializer} pair backed by an
 * {@link ObjectMapper} resolved through {@link BeanResolver}: the mapper is taken
 * from {@code objectMapperRef} when configured, otherwise the default mapper type
 * of the subclass is used.
 *
 * @param <C> the config class this provider accepts
 */
@ConditionalOnClass(ObjectMapper.class)
@RequiredArgsConstructor
public class JacksonSerdeProvider<C extends SerdeProperties> implements TestSerdeProvider<C> {
    protected final BeanResolver beanResolver;

    @Override
    public String getName() {
        return "jackson";
    }

    @Override
    public Object createSerializer(C config) {
        ObjectMapper mapper = beanResolver.resolve(config.getObjectMapperRef(), getMapperClass());
        return new JacksonSerializer<>(mapper);
    }

    @Override
    public Object createDeserializer(C config) {
        ObjectMapper mapper = beanResolver.resolve(config.getObjectMapperRef(), getMapperClass());
        return new JacksonDeserializer<>(mapper, config.getTargetClass());
    }

    /**
     * Returns the mapper type used when no explicit {@code objectMapperRef} is
     * configured. Subclasses override this to select a specific mapper, for example
     * {@code XmlMapper} or {@code YAMLMapper}.
     *
     * @return the default mapper class
     */
    protected Class<? extends ObjectMapper> getMapperClass() {
        return ObjectMapper.class;
    }
}
