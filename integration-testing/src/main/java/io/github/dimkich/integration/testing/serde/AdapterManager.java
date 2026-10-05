package io.github.dimkich.integration.testing.serde;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves an adapter chain for a raw serde source and adapts it to the requested converter types.
 *
 * <p>Adapters are sorted once at construction (see {@link TestSerdeComponent#compare}) and chains
 * are cached per source/config/input/output/context/role key. A source that already is a matching
 * {@link TestSerdeConverter} is returned as is; otherwise the first adapter whose
 * {@link TestSerdeAdapter#tryAdapt} succeeds wins. {@link #adapt} fails with a message listing the
 * known adapters when no chain produces a converter, while {@link #tryAdapt} returns {@code null}.
 */
public class AdapterManager {

    private final List<TestSerdeAdapter<?, ?, ?, ?, ?, ?>> adapters;
    private final ConcurrentHashMap<AdapterKey, List<TestSerdeAdapter<?, ?, ?, ?, ?, ?>>> chainCache =
            new ConcurrentHashMap<>();

    /**
     * Creates the manager.
     *
     * @param adapters all registered adapters, sorted by role and specificity
     */
    public AdapterManager(List<TestSerdeAdapter<?, ?, ?, ?, ?, ?>> adapters) {
        this.adapters = adapters.stream().sorted(TestSerdeAdapter::compare).toList();
    }

    /**
     * Adapts the source to a converter of the requested types.
     *
     * @param source       object returned by a provider or a previous adapter
     * @param props        serde configuration; must not be {@code null}, adapters are selected by its class
     * @param inputClass   requested input type
     * @param outputClass  requested output type
     * @param contextClass requested context type
     * @param role         requested role, or {@code null} for a universal component
     * @return the adapted converter
     * @throws IllegalArgumentException if the source or the configuration is {@code null}, or no adapter matches
     */
    public <I, O, C extends TestSerdeContext> TestSerdeConverter<I, O, C> adapt(
            Object source, TestSerdeProperties props, Class<I> inputClass, Class<O> outputClass, Class<C> contextClass,
            @Nullable ComponentRole role) {

        TestSerdeConverter<I, O, C> converter = tryAdapt(source, props, inputClass, outputClass, contextClass, role);
        if (converter == null) {
            Class<?> sourceClass = AopProxyUtils.ultimateTargetClass(source);
            throw new IllegalArgumentException(String.format(
                    "No adapter from [%s] to [%s] under config [%s]. Known adapters: %s",
                    sourceClass.getName(), outputClass.getName(), props.getClass().getSimpleName(),
                    knownAdapters(props.getClass())));
        }
        return converter;
    }

    /**
     * Adapts the source or returns {@code null} when no adapter matches.
     *
     * @param source       object returned by a provider or a previous adapter
     * @param props        serde configuration; must not be {@code null}, adapters are selected by its class
     * @param inputClass   requested input type
     * @param outputClass  requested output type
     * @param contextClass requested context type
     * @param role         requested role, or {@code null} for a universal component
     * @return the adapted converter, or {@code null} if no adapter matches
     * @throws IllegalArgumentException if the source or the configuration is {@code null}
     */
    @Nullable
    public <I, O, C extends TestSerdeContext> TestSerdeConverter<I, O, C> tryAdapt(
            Object source, TestSerdeProperties props, Class<I> inputClass, Class<O> outputClass, Class<C> contextClass,
            @Nullable ComponentRole role) {

        if (source == null) {
            throw new IllegalArgumentException("Source to adapt must not be null");
        }
        if (props == null) {
            throw new IllegalArgumentException("Serde configuration must not be null");
        }
        if (source instanceof TestSerdeConverter<?, ?, ?> converter
                && converter.satisfies(inputClass, outputClass, contextClass)) {
            return TestSerdeConverter.uncheckedCast(converter);
        }
        Class<?> sourceClass = AopProxyUtils.ultimateTargetClass(source);
        AdapterKey key = new AdapterKey(sourceClass, props.getClass(), inputClass, outputClass, contextClass, role);
        List<TestSerdeAdapter<?, ?, ?, ?, ?, ?>> chain = chainCache.computeIfAbsent(key, this::buildChain);
        for (TestSerdeAdapter<?, ?, ?, ?, ?, ?> adapter : chain) {
            TestSerdeConverter<I, O, C> converter =
                    adapter.tryAdapt(source, props, inputClass, outputClass, contextClass, role);
            if (converter == null) {
                continue;
            }
            return converter;
        }
        return null;
    }

    private List<TestSerdeAdapter<?, ?, ?, ?, ?, ?>> buildChain(AdapterKey key) {
        return adapters.stream()
                .filter(adapter -> adapter.matches(key.sourceClass(), key.propsClass(), key.contextClass(),
                        key.inputClass(), key.outputClass(), key.role()))
                .toList();
    }

    private List<String> knownAdapters(Class<?> propsClass) {
        return adapters.stream()
                .filter(adapter -> adapter.getPropertiesClass().isAssignableFrom(propsClass))
                .map(adapter -> String.format("%s [%s -> %s/%s]",
                        adapter.getClass().getName(),
                        adapter.getSourceClass().getName(),
                        adapter.getInputClassName(),
                        adapter.getOutputClassName()))
                .sorted()
                .toList();
    }

    private record AdapterKey(Class<?> sourceClass, Class<?> propsClass, Class<?> inputClass,
                              Class<?> outputClass, Class<?> contextClass, @Nullable ComponentRole role) {
    }
}
