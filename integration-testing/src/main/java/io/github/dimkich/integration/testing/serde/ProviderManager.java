package io.github.dimkich.integration.testing.serde;

import io.github.dimkich.integration.testing.serde.properties.ProviderProperties;
import org.springframework.lang.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves converters for providers declared by name through {@link ProviderProperties}
 * (e.g. {@code type: json} or a fully qualified class name).
 *
 * <p>Providers are grouped by name and sorted once at construction (see
 * {@link TestSerdeComponent#compare}); per-request chains are cached. {@link #resolve} throws when
 * the name is unknown or exists but not for the given configuration class, and returns
 * {@code null} when providers with this name exist but none creates a converter.
 */
public class ProviderManager {

    private static final Comparator<TestSerdeProviderFactory<?, ?, ?, ?, ?>> PROVIDER_ORDER =
            TestSerdeComponent::compare;

    private final Map<String, List<TestSerdeProviderFactory<?, ?, ?, ?, ?>>> namedProviders = new HashMap<>();

    private final ConcurrentHashMap<ProviderKey, List<TestSerdeProviderFactory<?, ?, ?, ?, ?>>> chainCache =
            new ConcurrentHashMap<>();

    /**
     * Creates the manager.
     *
     * @param providers all registered providers, grouped by {@link TestSerdeProviderFactory#getName()}
     */
    public ProviderManager(List<TestSerdeProviderFactory<?, ?, ?, ?, ?>> providers) {
        for (TestSerdeProviderFactory<?, ?, ?, ?, ?> provider : providers) {
            namedProviders.computeIfAbsent(provider.getName(), key -> new ArrayList<>()).add(provider);
        }
        namedProviders.replaceAll((key, value) -> value.stream().sorted(PROVIDER_ORDER).toList());
    }

    /**
     * Resolves a converter from the providers named by the configuration.
     *
     * @param config       provider configuration whose {@code type} names the provider
     * @param inputClass   requested input type
     * @param outputClass  requested output type
     * @param contextClass requested context type
     * @param role         requested role, or {@code null} for a universal component
     * @return the resolved converter, or {@code null} if no provider with this name creates one
     * @throws IllegalArgumentException if the name is unknown or not available for the configuration class
     */
    @Nullable
    public <I, O, C extends TestSerdeContext> TestSerdeConverter<I, O, C> resolve(
            ProviderProperties config, Class<I> inputClass, Class<O> outputClass, Class<C> contextClass,
            @Nullable ComponentRole role) {

        String name = config.getType();
        List<TestSerdeProviderFactory<?, ?, ?, ?, ?>> byName = namedProviders.get(name);
        if (byName == null || byName.stream()
                .noneMatch(provider -> provider.getPropertiesClass().isAssignableFrom(config.getClass()))) {
            throw unknownProvider(name, config.getClass());
        }
        ProviderKey key = new ProviderKey(name, config.getClass(), inputClass, outputClass, contextClass, role);
        for (TestSerdeProviderFactory<?, ?, ?, ?, ?> provider : chainCache.computeIfAbsent(key, this::buildChain)) {
            TestSerdeConverter<I, O, C> converter =
                    provider.tryCreate(config, inputClass, outputClass, contextClass, role);
            if (converter != null) {
                return converter;
            }
        }
        return null;
    }

    private List<TestSerdeProviderFactory<?, ?, ?, ?, ?>> buildChain(ProviderKey key) {
        return namedProviders.get(key.name()).stream()
                .filter(provider -> provider.matches(key.configClass(), key.contextClass(), key.inputClass(),
                        key.outputClass(), key.role()))
                .toList();
    }

    private IllegalArgumentException unknownProvider(String name, Class<?> configClass) {
        StringBuilder message = new StringBuilder(String.format(
                "Unknown serde provider [%s] for config [%s]. Known providers: %s.",
                name, configClass.getSimpleName(), namedProviders.keySet()));
        List<TestSerdeProviderFactory<?, ?, ?, ?, ?>> byName = namedProviders.get(name);
        if (byName != null) {
            message.append(String.format("%nNote: '%s' is only available with config: %s.",
                    name, byName.stream()
                            .map(TestSerdeComponent::getPropertiesClass)
                            .distinct()
                            .sorted(Comparator.comparing(Class::getName))
                            .map(Class::getSimpleName)
                            .toList()));
        }
        message.append(String.format(
                "%nIf you meant a Java class, use its fully qualified name, not a simple name."));
        return new IllegalArgumentException(message.toString());
    }

    private record ProviderKey(String name, Class<?> configClass, Class<?> inputClass, Class<?> outputClass,
                               Class<?> contextClass, @Nullable ComponentRole role) {
    }
}
