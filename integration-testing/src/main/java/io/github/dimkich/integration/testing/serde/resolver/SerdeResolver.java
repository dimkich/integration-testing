package io.github.dimkich.integration.testing.serde.resolver;

import io.github.dimkich.integration.testing.serde.SerdeProperties;
import io.github.dimkich.integration.testing.serde.TestSerdeProvider;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Resolves raw serializer/deserializer instances from a {@link SerdeProperties}
 * configuration: an existing Spring bean via {@code beanRef}, a registered provider
 * selected by name, or a dynamically created bean selected by fully qualified class name.
 *
 * <p>Resolved providers and dynamically created beans are cached, so repeated
 * resolutions with the same type and config class reuse the same instance.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class SerdeResolver {
    private final AutowireCapableBeanFactory beanFactory;
    private final ProviderRegistry registry;
    private final ConcurrentHashMap<ProviderKey, TestSerdeProvider<?>> providerCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<DynamicKey, Object> dynamicBeanCache = new ConcurrentHashMap<>();

    /**
     * Resolves a serializer for the given configuration.
     *
     * @param props serde configuration
     * @return a serializer instance, or {@code null} if the configuration is empty
     * @throws IllegalArgumentException if the configuration is invalid or the
     *                                  provider/class cannot be resolved
     */
    public Object resolveSerializer(SerdeProperties props) {
        return resolve(props, true);
    }

    /**
     * Resolves a deserializer for the given configuration.
     *
     * @param props serde configuration
     * @return a deserializer instance, or {@code null} if the configuration is empty
     * @throws IllegalArgumentException if the configuration is invalid or the
     *                                  provider/class cannot be resolved
     */
    public Object resolveDeserializer(SerdeProperties props) {
        return resolve(props, false);
    }

    @SuppressWarnings("unchecked")
    private Object resolve(SerdeProperties props, boolean serialize) {
        props.validate();

        if (props.hasBeanRef()) {
            if (log.isDebugEnabled()) {
                log.debug("resolve: beanRef=[{}]", props.getBeanRef());
            }
            return beanFactory.getBean(props.getBeanRef());
        }

        if (props.hasType()) {
            TestSerdeProvider<?> provider = providerCache.computeIfAbsent(
                    new ProviderKey(props.getType(), props.getClass()),
                    k -> registry.findBestProvider(k.getName(), k.getConfigClass())
            );

            if (provider != null) {
                if (log.isDebugEnabled()) {
                    log.debug("resolve: type=[{}] provider=[{}]",
                            props.getType(), provider.getClass().getSimpleName());
                }
                return serialize
                        ? ((TestSerdeProvider<SerdeProperties>) provider).createSerializer(props)
                        : ((TestSerdeProvider<SerdeProperties>) provider).createDeserializer(props);
            }

            if (log.isDebugEnabled()) {
                log.debug("resolve: type=[{}] no registered provider, dynamic bean fallback",
                        props.getType());
            }
            return createDynamicBean(props);
        }

        return null;
    }

    /**
     * Resolves a serializer/deserializer bean by provider name or fully qualified
     * class name. The created bean is cached, so repeated resolutions reuse the
     * same instance.
     *
     * @param props serde configuration with the type to resolve
     * @return cached bean instance
     */
    private Object createDynamicBean(SerdeProperties props) {
        DynamicKey key = new DynamicKey(props.getType(), props.getClass());
        return dynamicBeanCache.computeIfAbsent(key,
                k -> instantiate(k.name, k.configClass));
    }

    /**
     * Creates a new bean instance by fully qualified class name. Not called for
     * simple names without a matching registered provider.
     *
     * @param type provider name or fully qualified class name
     * @param configClass config class used to build a provider-specific message
     * @return new bean instance
     */
    private Object instantiate(String type, Class<?> configClass) {
        if (!type.contains(".")) {
            Set<String> known = registry.getProviderNames(configClass);
            StringBuilder msg = new StringBuilder(String.format(
                    "Unknown serde provider [%s] for config [%s]. " +
                            "Known providers for this config: %s.",
                    type, configClass.getSimpleName(), known));
            List<Class<?>> otherConfigs = registry.getProviderConfigClasses(type);
            if (!otherConfigs.isEmpty()) {
                msg.append(String.format("%nNote: '%s' is only available with config: [%s].",
                        type, otherConfigs.stream()
                                .map(Class::getSimpleName)
                                .collect(Collectors.toList())));
            }
            msg.append(String.format("%nIf you meant a Java class, use its fully qualified name " +
                    "(e.g. 'com.example.MySerializer'), not a simple name."));
            throw new IllegalArgumentException(msg.toString());
        }
        Class<?> clazz;
        try {
            clazz = Class.forName(type);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(String.format(
                    "Serde class [%s] not found, and no provider with this name. " +
                            "Known providers for this config: %s.",
                    type, registry.getProviderNames(configClass)), e);
        }
        return beanFactory.createBean(clazz);
    }

    @Data
    private static class ProviderKey {
        private final String name;
        private final Class<?> configClass;
    }

    private record DynamicKey(String name, Class<?> configClass) {
    }
}
