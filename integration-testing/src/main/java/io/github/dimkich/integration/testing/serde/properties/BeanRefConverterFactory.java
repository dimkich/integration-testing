package io.github.dimkich.integration.testing.serde.properties;

import io.github.dimkich.integration.testing.serde.AdapterManager;
import io.github.dimkich.integration.testing.serde.ComponentRole;
import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.core.annotation.Order;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Resolves a converter from a Spring bean referenced by {@code beanRef}: the bean is fetched and
 * adapted to the requested types via {@link AdapterManager}.
 *
 * <p>Returns {@code null} when {@code beanRef} is not set. {@link Order}{@code (2000)} places this
 * factory between {@link ProviderConverterFactory} and {@link ClassRefConverterFactory} so that
 * equally specific factories are evaluated deterministically.
 *
 * @see BeanRefProperties
 */
@Getter
@Order(2000)
@RequiredArgsConstructor
public class BeanRefConverterFactory extends UniversalConverterFactory<BeanRefProperties> {

    @Getter(AccessLevel.NONE)
    private final AutowireCapableBeanFactory beanFactory;

    @Getter(AccessLevel.NONE)
    private final AdapterManager adapterManager;

    private final Class<BeanRefProperties> propertiesClass = BeanRefProperties.class;

    @Override
    @Nullable
    public TestSerdeConverter<Object, Object, TestSerdeContext> create(BeanRefProperties config, Class<Object> inputClass,
                                                                       Class<Object> outputClass,
                                                                       Class<TestSerdeContext> contextClass,
                                                                       @Nullable ComponentRole role) {
        if (!StringUtils.hasText(config.getBeanRef())) {
            return null;
        }
        Object source;
        try {
            source = beanFactory.getBean(config.getBeanRef());
        } catch (NoSuchBeanDefinitionException e) {
            throw new IllegalArgumentException(String.format(
                    "No bean named [%s] found. Check 'beanRef' in configuration.", config.getBeanRef()), e);
        }
        return adapterManager.adapt(source, config, inputClass, outputClass, contextClass, role);
    }
}
