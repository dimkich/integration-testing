package io.github.dimkich.integration.testing.serde.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dimkich.integration.testing.serde.BeanResolver;
import io.github.dimkich.integration.testing.serde.SerdeProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

/**
 * JSON provider registered under the name {@code json}; serializes values with
 * Jackson {@link ObjectMapper}.
 */
@ConditionalOnClass(ObjectMapper.class)
public class JsonSerdeProvider extends JacksonSerdeProvider<SerdeProperties> {

    /**
     * Creates the provider.
     *
     * @param beanResolver resolves the {@link ObjectMapper} bean
     */
    public JsonSerdeProvider(BeanResolver beanResolver) {
        super(beanResolver);
    }

    @Override
    public String getName() {
        return "json";
    }
}
