package io.github.dimkich.integration.testing.serde;

import io.github.dimkich.integration.testing.serde.adapter.AdapterScanner;
import io.github.dimkich.integration.testing.serde.adapter.SerdeAdapterResolver;
import io.github.dimkich.integration.testing.serde.providers.*;
import io.github.dimkich.integration.testing.serde.resolver.ProviderRegistry;
import io.github.dimkich.integration.testing.serde.resolver.SerdeResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Spring configuration of the serde subsystem: registers the resolver, adapter
 * scanner, built-in providers and supporting beans. Imported by
 * {@code IntegrationTestConfig}.
 */
@Configuration
@Import({
        BeanResolver.class,
        SerdeResolver.class,
        SerdeAdapterResolver.class,
        SerdeManager.class,
        AdapterScanner.class,
        ProviderRegistry.class,
        SerdeGenericResolver.class,
        TypeConverter.class,
        StringSerdeProvider.class,
        ByteArraySerdeProvider.class,
        JsonSerdeProvider.class,
        XmlSerdeProvider.class,
        YamlSerdeProvider.class
})
public class SerdeConfig {
}
