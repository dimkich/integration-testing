package io.github.dimkich.integration.testing.serde.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dimkich.integration.testing.serde.BeanResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

/**
 * Provider {@code json}: Jackson JSON deserializer, registered when Jackson is on the classpath.
 */
@ConditionalOnClass(ObjectMapper.class)
public class JsonDeserializerFactory extends JacksonDeserializerFactory {

    /**
     * Creates the factory.
     *
     * @param beanResolver resolves the mapper bean of the provider
     */
    public JsonDeserializerFactory(BeanResolver beanResolver) {
        super(beanResolver, "json");
    }
}
