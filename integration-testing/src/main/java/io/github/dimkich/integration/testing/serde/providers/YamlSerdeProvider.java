package io.github.dimkich.integration.testing.serde.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.github.dimkich.integration.testing.serde.BeanResolver;
import io.github.dimkich.integration.testing.serde.SerdeProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

/**
 * YAML provider registered under the name {@code yaml}; serializes values with Jackson
 * {@link YAMLMapper}.
 */
@ConditionalOnClass(YAMLMapper.class)
public class YamlSerdeProvider extends JacksonSerdeProvider<SerdeProperties> {

    /**
     * Creates the provider.
     *
     * @param beanResolver resolves the {@link YAMLMapper} bean
     */
    public YamlSerdeProvider(BeanResolver beanResolver) {
        super(beanResolver);
    }

    @Override
    public String getName() {
        return "yaml";
    }

    @Override
    protected Class<? extends ObjectMapper> getMapperClass() {
        return YAMLMapper.class;
    }
}
