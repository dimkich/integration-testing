package io.github.dimkich.integration.testing.serde.adapter;

import io.github.dimkich.integration.testing.serde.SerdeGenericResolver;
import io.github.dimkich.integration.testing.serde.TestSerdeAdapter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.util.*;

/**
 * Collects and indexes all registered {@link TestSerdeAdapter} beans.
 *
 * <p>Adapters are indexed by exact source class into a list of adapter metadata.
 * The outer map is sorted by source class name to ensure deterministic traversal.
 * Initialization uses thread-safe lazy loading via double-checked locking with
 * a volatile store to guarantee safe publication across threads.
 *
 * <p>Each (source, target, properties) triple may hold at most one adapter bean;
 * duplicates declared for the exact same triple are rejected at initialization.
 * Adapters sharing the same (source, target) but having different properties classes
 * are allowed to coexist.
 */
@RequiredArgsConstructor
public class AdapterScanner {
    private final ConfigurableListableBeanFactory beanFactory;
    private final SerdeGenericResolver genericResolver;
    private volatile Map<Class<?>, List<AdapterMetadata>> adapterMap;

    /**
     * Finds all registered adapters for the exact source class whose target class
     * is assignable from {@code targetClass} and whose config class is compatible with
     * {@code propertiesClass}.
     *
     * @param sourceClass exact source class to look up
     * @param targetClass requested target type
     * @param propertiesClass configuration class
     * @return matching adapters for the specified source
     */
    public List<AdapterMetadata> findAdapters(Class<?> sourceClass, Class<?> targetClass, Class<?> propertiesClass) {
        ensureInitialized();
        List<AdapterMetadata> adapters = adapterMap.get(sourceClass);
        if (adapters == null) {
            return List.of();
        }
        List<AdapterMetadata> candidates = new ArrayList<>();
        for (AdapterMetadata meta : adapters) {
            if (targetClass.isAssignableFrom(meta.getTargetClass())
                    && meta.getPropertiesClass().isAssignableFrom(propertiesClass)) {
                candidates.add(meta);
            }
        }
        return candidates;
    }

    /**
     * Returns the source types of all adapters that can produce the given target
     * under the given config class. Used for diagnostic error messages.
     *
     * @param targetClass requested target type
     * @param propertiesClass configuration class
     * @return source class simple names in sorted order
     */
    public List<Class<?>> findSupportedSources(Class<?> targetClass, Class<?> propertiesClass) {
        ensureInitialized();
        TreeSet<Class<?>> sources = new TreeSet<>(Comparator.comparing(Class::getSimpleName));
        for (Map.Entry<Class<?>, List<AdapterMetadata>> entry : adapterMap.entrySet()) {
            for (AdapterMetadata meta : entry.getValue()) {
                if (targetClass.isAssignableFrom(meta.getTargetClass())
                        && meta.getPropertiesClass().isAssignableFrom(propertiesClass)) {
                    sources.add(entry.getKey());
                    break;
                }
            }
        }
        return List.copyOf(sources);
    }

    /**
     * Ensures that {@link #adapterMap} is initialized exactly once in a thread-safe manner
     * using double-checked locking.
     */
    private void ensureInitialized() {
        if (adapterMap == null) {
            synchronized (this) {
                if (adapterMap == null) {
                    init();
                }
            }
        }
    }

    private void init() {
        Map<Class<?>, List<AdapterMetadata>> map = new TreeMap<>(Comparator.comparing(Class::getName));
        Map<AdapterKey, AdapterMetadata> seen = new HashMap<>();

        genericResolver.findAndResolveGenerics(TestSerdeAdapter.class)
                .forEach(resolved -> {
                    Class<?>[] generics = resolved.getGenerics();
                    AdapterMetadata meta = new AdapterMetadata(
                            generics[0], generics[1], generics[2],
                            beanFactory.getBean(resolved.getBeanName(), TestSerdeAdapter.class));

                    AdapterKey key = new AdapterKey(
                            meta.getSourceClass(),
                            meta.getTargetClass(),
                            meta.getPropertiesClass());

                    AdapterMetadata existing = seen.putIfAbsent(key, meta);
                    if (existing != null && existing != meta) {
                        throw new IllegalArgumentException(String.format(
                                "Duplicate serde adapters for source [%s], target [%s], config [%s]: [%s] and [%s]. " +
                                        "Each (source, target, config) triple must be unique.",
                                meta.getSourceClass().getSimpleName(),
                                meta.getTargetClass().getSimpleName(),
                                meta.getPropertiesClass().getSimpleName(),
                                existing.getInstance().getClass().getName(),
                                meta.getInstance().getClass().getName()
                        ));
                    }

                    map.computeIfAbsent(meta.getSourceClass(), k -> new ArrayList<>()).add(meta);
                });
        adapterMap = map;
    }

    @Value
    private static class AdapterKey {
        Class<?> sourceClass;
        Class<?> targetClass;
        Class<?> propertiesClass;
    }
}
