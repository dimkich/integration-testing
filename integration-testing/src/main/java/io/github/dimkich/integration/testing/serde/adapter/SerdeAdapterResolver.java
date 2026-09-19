package io.github.dimkich.integration.testing.serde.adapter;

import io.github.dimkich.integration.testing.serde.SerdeProperties;
import io.github.dimkich.integration.testing.serde.TestSerdeAdapter;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopProxyUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves a {@link TestSerdeAdapter} for a concrete bean, target type and
 * config class, then adapts the bean.
 *
 * <p>Candidate adapters are found by a BFS over the bean class hierarchy
 * (superclasses and interfaces). The BFS is deterministic: every level is fully
 * explored before the next one, and interfaces are visited in sorted order.
 * The best candidate per level is selected by target and config specificity — see
 * {@link #findAdapter}. The chosen adapter is cached per (source, target,
 * config) key.
 *
 * <h3>Diagnosing ambiguous adapters</h3>
 *
 * <p>{@link #pick} throws {@link IllegalArgumentException} when two or more adapters
 * sit on the same BFS depth and neither dominates by target nor by config specificity.
 * Common causes:
 * <ul>
 *   <li>two adapters registered for unrelated interfaces that a bean implements at the same level;</li>
 *   <li>a user-registered adapter for a superinterface alongside a built-in adapter for a subinterface
 *       at the same depth (introduce a more specific subclass or a config-class subtype to disambiguate).</li>
 * </ul>
 * The exception message lists all candidates; extend one of them to a broader target or
 * more specific config class to break the tie.
 */
@Slf4j
@RequiredArgsConstructor
public class SerdeAdapterResolver {
    private final AdapterScanner scanner;
    private final Map<CacheKey, AdapterMetadata> adapterCache = new ConcurrentHashMap<>();

    /**
     * Adapts the given bean to the requested target type.
     *
     * <p>If the bean already is an instance of the target type, it is returned
     * as is. Otherwise, the best matching adapter is applied. Selection
     * priority, in order:
     * <ol>
     *   <li><b>Closest source</b> — the adapter whose source class is nearest
     *       to the bean class in the hierarchy (fewest BFS steps);</li>
     *   <li><b>Closest (broadest) target</b> — among adapters of the same depth, the
     *       one whose target class is assignable from all others' target
     *       classes;</li>
     *   <li><b>Most specific configuration</b> — among adapters with the same target
     *       class, the one whose configuration class is a strict subtype of others'
     *       configuration classes (e.g. module-specific config overriding core base config);</li>
     *   <li>If several equally specific candidates remain, an
     *       {@link IllegalArgumentException} is thrown listing all of them.</li>
     * </ol>
     *
     * @param bean object to adapt (created by a serde provider)
     * @param targetType requested target type
     * @param properties configuration
     * @param <T> target type
     * @return adapted object
     */
    public <T> T adapt(Object bean, Class<T> targetType, SerdeProperties properties) {
        if (targetType.isInstance(bean)) {
            if (log.isDebugEnabled()) {
                log.debug("no adapter needed: [{}] is already [{}]",
                        bean.getClass().getSimpleName(), targetType.getSimpleName());
            }
            return targetType.cast(bean);
        }

        Class<? extends SerdeProperties> propertiesClass = properties.getClass();
        Class<?> beanClass = AopProxyUtils.ultimateTargetClass(bean);
        CacheKey key = new CacheKey(beanClass, targetType, propertiesClass);
        AdapterMetadata meta = adapterCache.get(key);
        boolean cacheHit = meta != null;

        if (!cacheHit) {
            meta = findAdapter(beanClass, targetType, propertiesClass);
            if (meta != null) {
                adapterCache.put(key, meta);
            }
        }

        if (meta == null) {
            List<Class<?>> supported = scanner.findSupportedSources(targetType, propertiesClass);
            if (log.isDebugEnabled()) {
                log.debug("no adapter: [{}] -> [{}] under [{}]; supported sources: {}",
                        bean.getClass().getSimpleName(),
                        targetType.getSimpleName(),
                        propertiesClass.getSimpleName(),
                        supported.stream().map(Class::getSimpleName).sorted().toList());
            }
            throw new IllegalArgumentException(String.format(
                    "No adapter found from [%s] to [%s] under configuration [%s]. Supported source types: %s",
                    bean.getClass().getName(), targetType.getName(), propertiesClass.getSimpleName(),
                    supported.stream().map(Class::getSimpleName).toList()
            ));
        }

        if (!meta.getSourceClass().isInstance(bean)) {
            throw new IllegalArgumentException(String.format(
                    "Cannot adapt [%s] to [%s]: adapter requires source [%s]",
                    bean.getClass().getName(), targetType.getName(), meta.getSourceClass().getName()
            ));
        }

        if (log.isDebugEnabled()) {
            log.debug("adapt [{}] -> [{}] via [{}]{}",
                    bean.getClass().getSimpleName(),
                    targetType.getSimpleName(),
                    meta.getInstance().getClass().getSimpleName(),
                    cacheHit ? " (cached)" : "");
        }

        return targetType.cast(invoke(meta, bean, properties));
    }

    /**
     * Finds the best adapter for the given source, target and config class.
     *
     * <p>The search is a level-by-level BFS over the class hierarchy. All
     * candidates of the same depth are collected first, then the most specific
     * one is picked — so the result does not depend on the traversal order
     * within a level.
     *
     * @param sourceClass bean class to adapt
     * @param targetClass requested target type
     * @param propertiesClass configuration class
     * @return matching adapter, or {@code null} if none
     * @see #pick(List, Class, Class)
     */
    private AdapterMetadata findAdapter(Class<?> sourceClass, Class<?> targetClass, Class<? extends SerdeProperties> propertiesClass) {
        Queue<Class<?>> queue = new LinkedList<>();
        Set<Class<?>> visited = new HashSet<>();
        queue.add(sourceClass);
        int depth = 0;

        while (!queue.isEmpty()) {
            int levelSize = queue.size();
            List<AdapterMetadata> levelCandidates = new ArrayList<>();
            for (int i = 0; i < levelSize; i++) {
                Class<?> current = queue.poll();
                if (current == null) {
                    continue;
                }
                if (!visited.add(current)) {
                    continue;
                }
                List<AdapterMetadata> found = scanner.findAdapters(current, targetClass, propertiesClass);
                if (log.isDebugEnabled() && !found.isEmpty()) {
                    log.debug("  BFS depth {}: [{}] -> {} candidate(s): {}",
                            depth, current.getSimpleName(), found.size(),
                            found.stream().map(m -> m.getInstance().getClass().getSimpleName())
                                    .sorted().toList());
                }
                levelCandidates.addAll(found);

                Class<?> superClass = current.getSuperclass();
                if (superClass != null && !visited.contains(superClass)) {
                    queue.add(superClass);
                }
                Class<?>[] interfaces = current.getInterfaces();
                Arrays.sort(interfaces, Comparator.comparing(Class::getName));
                for (Class<?> iface : interfaces) {
                    if (!visited.contains(iface)) {
                        queue.add(iface);
                    }
                }
            }
            if (!levelCandidates.isEmpty()) {
                return pick(levelCandidates, sourceClass, targetClass);
            }
            depth++;
        }

        if (log.isDebugEnabled()) {
            log.debug("  BFS exhausted for [{}] -> [{}], no candidate",
                    sourceClass.getSimpleName(), targetClass.getSimpleName());
        }
        return null;
    }

    /**
     * Selects the most specific adapter among the candidates of the same BFS depth.
     *
     * <p>{@code b} dominates {@code a} when {@code b} is strictly more specific and should win the
     * tie-break, see {@link #dominates}. A candidate survives as long as no other candidate
     * dominates it.
     *
     * <p>If exactly one candidate dominates, it is returned; otherwise an
     * {@link IllegalArgumentException} is thrown listing all ambiguous candidates.
     *
     * @param candidates candidate adapters (all the same BFS depth)
     * @param sourceClass bean class used for the error message
     * @param targetClass requested target type used for the error message
     * @return the most specific adapter
     */
    private AdapterMetadata pick(List<AdapterMetadata> candidates, Class<?> sourceClass, Class<?> targetClass) {
        if (candidates.size() == 1) {
            if (log.isDebugEnabled()) {
                log.debug("  pick: single candidate [{}]",
                        candidates.get(0).getInstance().getClass().getSimpleName());
            }
            return candidates.get(0);
        }

        List<AdapterMetadata> maximal = candidates.stream()
                .filter(a -> candidates.stream().noneMatch(b -> dominates(b, a)))
                .toList();

        if (log.isDebugEnabled()) {
            log.debug("  pick: {} candidate(s) on the same BFS level:", candidates.size());
            for (AdapterMetadata a : candidates) {
                boolean dominated = candidates.stream().anyMatch(b -> dominates(b, a));
                log.debug("    [{}] target=[{}] config=[{}] {}",
                        a.getInstance().getClass().getSimpleName(),
                        a.getTargetClass().getSimpleName(),
                        a.getPropertiesClass().getSimpleName(),
                        dominated ? "(dominated)" : "(maximal)");
            }
        }

        if (maximal.size() == 1) {
            if (log.isDebugEnabled()) {
                log.debug("  pick: winner [{}]",
                        maximal.get(0).getInstance().getClass().getSimpleName());
            }
            return maximal.get(0);
        }

        boolean sameTarget = maximal.stream()
                .map(AdapterMetadata::getTargetClass)
                .distinct()
                .count() == 1;
        String detail = sameTarget
                ? "all candidates share target [" + maximal.get(0).getTargetClass().getSimpleName() + "]"
                : "candidates have incomparable target classes";

        if (log.isDebugEnabled()) {
            log.debug("  pick: ambiguity — {} maximal candidate(s), {}",
                    maximal.size(), detail);
        }

        throw new IllegalArgumentException(String.format(
                "Ambiguous serde adapters for source [%s] and target [%s]: %s (%s). " +
                        "Declare a more specific adapter to disambiguate.",
                sourceClass.getName(), targetClass.getName(),
                maximal.stream().map(m -> m.getInstance().getClass().getName()).sorted().toList(),
                detail));
    }

    /**
     * {@code b} dominates {@code a} when {@code b} is strictly more specific and
     * should win the tie-break.
     * <ul>
     *   <li><b>Targets differ:</b> {@code b} wins when its target is a strict
     *       supertype of {@code a}'s (broader/closest to the requested target).</li>
     *   <li><b>Targets equal:</b> {@code b} wins when its config class is a strict
     *       subtype of {@code a}'s (narrower config).</li>
     * </ul>
     * Incomparable targets or configs leave neither side dominating; such cases
     * surface as {@link #pick} ambiguity instead of being silently ranked.
     */
    private static boolean dominates(AdapterMetadata b, AdapterMetadata a) {
        if (b.getTargetClass() != a.getTargetClass()) {
            return b.getTargetClass().isAssignableFrom(a.getTargetClass())
                    && !a.getTargetClass().isAssignableFrom(b.getTargetClass());
        }
        return b.getPropertiesClass() != a.getPropertiesClass()
                && a.getPropertiesClass().isAssignableFrom(b.getPropertiesClass())
                && !b.getPropertiesClass().isAssignableFrom(a.getPropertiesClass());
    }

    @SuppressWarnings("unchecked")
    private Object invoke(AdapterMetadata meta, Object bean, SerdeProperties properties) {
        return ((TestSerdeAdapter<Object, Object, SerdeProperties>) meta.getInstance()).adapt(bean, properties);
    }

    @Data
    private static class CacheKey {
        private final Class<?> sourceClass;
        private final Class<?> targetClass;
        private final Class<? extends SerdeProperties> propertiesClass;
    }
}