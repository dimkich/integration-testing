package io.github.dimkich.integration.testing.serde.resolver;

import io.github.dimkich.integration.testing.serde.SerdeGenericResolver;
import io.github.dimkich.integration.testing.serde.TestSerdeProvider;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Registry of {@link TestSerdeProvider} beans, indexed by provider name and
 * configuration class.
 *
 * <p>Providers are registered against a concrete config class (a subclass of
 * {@code SerdeProperties}). Because the config hierarchy is a single-inheritance
 * chain (classes only, no interfaces), provider resolution is a direct walk up
 * the superclass chain: the first provider found for the required config class
 * or its closest ancestor wins. No specificity ranking is needed — in a linear
 * hierarchy there cannot be two equally specific providers for the same name.
 *
 * <p>Two providers registered under the same name for the same config class are
 * rejected at startup: the second registration would silently overwrite the
 * first, so the duplicate is reported explicitly.
 *
 * <p>Example: given {@code spring-json} registered for {@code RecordProperties}
 * and {@code json} registered for {@code SerdeProperties}, a request for
 * {@code RecordProperties} returns {@code spring-json} (closest on the chain),
 * and a request for {@code SerdeProperties} returns {@code json}.
 */
@RequiredArgsConstructor
public class ProviderRegistry {
    private final ConfigurableListableBeanFactory beanFactory;
    private final SerdeGenericResolver genericResolver;

    /**
     * Provider name -> (config class -> provider).
     * <p>Outer map is sorted case-insensitively for deterministic iteration.
     */
    private Map<String, Map<Class<?>, TestSerdeProvider<?>>> registry;

    @PostConstruct
    private void init() {
        Map<String, Map<Class<?>, TestSerdeProvider<?>>> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        genericResolver.findAndResolveGenerics(TestSerdeProvider.class)
                .forEach(resolved -> {
                    Class<?> configClass = resolved.getGenerics()[0];
                    TestSerdeProvider<?> provider = beanFactory.getBean(
                            resolved.getBeanName(), TestSerdeProvider.class);

                    Map<Class<?>, TestSerdeProvider<?>> byConfig = map.computeIfAbsent(
                            provider.getName(), k -> new LinkedHashMap<>());

                    TestSerdeProvider<?> existing = byConfig.putIfAbsent(configClass, provider);
                    if (existing != null && existing != provider) {
                        throw new IllegalStateException(String.format(
                                "Duplicate serde providers with name [%s] and config class [%s]: [%s] and [%s]. " +
                                        "Each (name, config) pair must be unique.",
                                provider.getName(), configClass.getName(),
                                existing.getClass().getName(), provider.getClass().getName()));
                    }
                });

        this.registry = Collections.unmodifiableMap(map);
    }

    /**
     * Returns names of registered serde providers applicable to the given config class.
     * <p>A provider is applicable when its config class lies on the superclass chain
     * of {@code configClass}.
     *
     * @param configClass config class ({@code SerdeProperties} or a subclass)
     * @return set of provider names valid for this config
     */
    public Set<String> getProviderNames(Class<?> configClass) {
        return registry.entrySet().stream()
                .filter(e -> findProviderInChain(e.getValue(), configClass) != null)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    /**
     * Returns config classes for which the provider with the given name is registered.
     *
     * @param name provider name
     * @return config classes registered for this name, empty if the name is unknown
     */
    public List<Class<?>> getProviderConfigClasses(String name) {
        Map<Class<?>, TestSerdeProvider<?>> byConfig = registry.get(name);
        return byConfig == null ? List.of() : List.copyOf(byConfig.keySet());
    }

    /**
     * Finds the provider for the given name applicable to the required config class.
     *
     * <p>The lookup walks up the superclass chain of {@code requiredConfigClass}.
     * The first provider registered for a class on this chain wins — that is the
     * most specific provider for the requested config.
     *
     * @param typeName provider name
     * @param requiredConfigClass requested config class
     * @return the matching provider, or {@code null} if none matches
     */
    public TestSerdeProvider<?> findBestProvider(String typeName, Class<?> requiredConfigClass) {
        Map<Class<?>, TestSerdeProvider<?>> byConfig = registry.get(typeName);
        if (byConfig == null) {
            return null;
        }
        return findProviderInChain(byConfig, requiredConfigClass);
    }

    private static TestSerdeProvider<?> findProviderInChain(
            Map<Class<?>, TestSerdeProvider<?>> byConfig, Class<?> configClass) {
        for (Class<?> current = configClass;
             current != null && current != Object.class;
             current = current.getSuperclass()) {
            TestSerdeProvider<?> provider = byConfig.get(current);
            if (provider != null) {
                return provider;
            }
        }
        return null;
    }
}
