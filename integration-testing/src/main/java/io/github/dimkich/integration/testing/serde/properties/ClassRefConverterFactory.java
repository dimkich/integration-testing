package io.github.dimkich.integration.testing.serde.properties;

import io.github.dimkich.integration.testing.serde.AdapterManager;
import io.github.dimkich.integration.testing.serde.ComponentRole;
import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.core.annotation.Order;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Resolves a converter from a fully qualified class name in {@code type}: the class is instantiated
 * by Spring and adapted to the requested types via {@link AdapterManager}.
 *
 * <p>Returns {@code null} when {@code type} is not set or does not contain a dot (i.e. is a
 * provider name handled by {@link ProviderConverterFactory}).
 *
 * @see ClassRefProperties
 */
@Getter
@Order(3000)
@RequiredArgsConstructor
public class ClassRefConverterFactory extends UniversalConverterFactory<ClassRefProperties> {

    @Getter(AccessLevel.NONE)
    private final AutowireCapableBeanFactory beanFactory;

    @Getter(AccessLevel.NONE)
    private final AdapterManager adapterManager;

    private final Class<ClassRefProperties> propertiesClass = ClassRefProperties.class;

    @Override
    @Nullable
    public TestSerdeConverter<Object, Object, TestSerdeContext> create(ClassRefProperties config, Class<Object> inputClass,
                                                                       Class<Object> outputClass,
                                                                       Class<TestSerdeContext> contextClass,
                                                                       @Nullable ComponentRole role) {
        String type = config.getType();
        if (!StringUtils.hasText(type) || !type.contains(".")) {
            return null;
        }
        Class<?> clazz;
        try {
            clazz = Class.forName(type);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(String.format(
                    "Class [%s] not found. If you meant a provider, its name must not contain a dot.", type));
        }
        Object source = beanFactory.createBean(clazz);
        return adapterManager.adapt(source, config, inputClass, outputClass, contextClass, role);
    }
}
