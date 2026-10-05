package io.github.dimkich.integration.testing.serde.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.github.dimkich.integration.testing.serde.BeanResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

/**
 * Provider {@code yaml}: Jackson YAML serializer backed by {@link YAMLMapper}, registered when
 * Jackson YAML is on the classpath.
 */
@ConditionalOnClass(YAMLMapper.class)
public class YamlSerializerFactory extends JacksonSerializerFactory {

    /**
     * Creates the factory.
     *
     * @param beanResolver resolves the mapper bean of the provider
     */
    public YamlSerializerFactory(BeanResolver beanResolver) {
        super(beanResolver, "yaml");
    }

    @Override
    protected Class<? extends ObjectMapper> getMapperClass() {
        return YAMLMapper.class;
    }
}
