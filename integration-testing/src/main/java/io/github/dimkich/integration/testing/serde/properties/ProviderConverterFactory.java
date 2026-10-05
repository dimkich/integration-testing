package io.github.dimkich.integration.testing.serde.properties;

import io.github.dimkich.integration.testing.serde.ComponentRole;
import io.github.dimkich.integration.testing.serde.ProviderManager;
import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Resolves a converter from the provider named by {@code type} via {@link ProviderManager}.
 *
 * <p>Returns {@code null} when {@code type} is not set or contains a dot (i.e. is a class name
 * handled by {@link ClassRefConverterFactory}).
 *
 * @see ProviderProperties
 */
@Getter
@Order(1000)
@RequiredArgsConstructor
public class ProviderConverterFactory extends UniversalConverterFactory<ProviderProperties> {

    @Getter(AccessLevel.NONE)
    private final ProviderManager providerManager;

    private final Class<ProviderProperties> propertiesClass = ProviderProperties.class;

    @Override
    @Nullable
    public TestSerdeConverter<Object, Object, TestSerdeContext> create(ProviderProperties config, Class<Object> inputClass,
                                                                       Class<Object> outputClass,
                                                                       Class<TestSerdeContext> contextClass,
                                                                       @Nullable ComponentRole role) {
        String name = config.getType();
        if (!StringUtils.hasText(name) || name.contains(".")) {
            return null;
        }
        return providerManager.resolve(config, inputClass, outputClass, contextClass, role);
    }
}
