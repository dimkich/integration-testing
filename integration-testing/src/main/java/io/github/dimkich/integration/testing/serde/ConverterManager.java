package io.github.dimkich.integration.testing.serde;

import org.springframework.lang.Nullable;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves a serde converter from the registered converter factories for the requested types,
 * configuration and role.
 *
 * <p>Factories are sorted once at construction (see {@link TestSerdeComponent#compare}) and chains
 * are cached per request key. The first factory that creates a converter wins, and the result is
 * passed through {@link DecoratorManager}. {@link #resolveOrNull} returns {@code null} when nothing
 * matches, while {@link #resolve} fails with a descriptive error.
 */
public class ConverterManager {

    private final List<TestSerdeConverterFactory<?, ?, ?, ?, ?>> factories;

    private final DecoratorManager decoratorManager;

    private final ConcurrentHashMap<FactoryKey, List<TestSerdeConverterFactory<?, ?, ?, ?, ?>>> factoryChains =
            new ConcurrentHashMap<>();

    /**
     * Creates the manager.
     *
     * @param factories        converter factories to search, sorted by role and specificity
     * @param decoratorManager manager applied to every created converter
     */
    public ConverterManager(List<TestSerdeConverterFactory<?, ?, ?, ?, ?>> factories,
                            DecoratorManager decoratorManager) {
        this.factories = factories.stream().sorted(TestSerdeComponent::compare).toList();
        this.decoratorManager = decoratorManager;
    }

    /**
     * Resolves a converter of the requested types or fails when none is configured.
     *
     * @param inputClass   requested input type
     * @param outputClass  requested output type
     * @param contextClass requested context type
     * @param props        serde configuration the converter is created from
     * @param role         requested role, or {@code null} for a universal component
     * @return the resolved converter, already decorated
     * @throws IllegalArgumentException if no factory creates a matching converter
     */
    public <I, O, C extends TestSerdeContext> TestSerdeConverter<I, O, C> resolve(
            Class<I> inputClass, Class<O> outputClass, Class<C> contextClass, TestSerdeProperties props,
            @Nullable ComponentRole role) {

        TestSerdeConverter<I, O, C> converter = resolveOrNull(inputClass, outputClass, contextClass, props, role);
        if (converter == null) {
            throw new IllegalArgumentException(String.format(
                    "No converter for [%s -> %s] under config [%s]",
                    inputClass.getSimpleName(), outputClass.getSimpleName(), props.getClass().getSimpleName()));
        }
        return converter;
    }

    /**
     * Resolves a converter of the requested types, or returns {@code null} when none is configured.
     *
     * @param inputClass   requested input type
     * @param outputClass  requested output type
     * @param contextClass requested context type
     * @param props        serde configuration the converter is created from
     * @param role         requested role, or {@code null} for a universal component
     * @return the resolved converter, already decorated, or {@code null} if none matches
     */
    @Nullable
    public <I, O, C extends TestSerdeContext> TestSerdeConverter<I, O, C> resolveOrNull(
            Class<I> inputClass, Class<O> outputClass, Class<C> contextClass, TestSerdeProperties props,
            @Nullable ComponentRole role) {

        FactoryKey key = new FactoryKey(props.getClass(), inputClass, outputClass, contextClass, role);
        for (TestSerdeConverterFactory<?, ?, ?, ?, ?> factory : factoryChains.computeIfAbsent(key, this::buildChain)) {
            TestSerdeConverter<I, O, C> converter =
                    factory.tryCreate(props, inputClass, outputClass, contextClass, role);
            if (converter == null) {
                continue;
            }
            return decoratorManager.decorate(converter, props, inputClass, outputClass, contextClass);
        }
        return null;
    }

    private List<TestSerdeConverterFactory<?, ?, ?, ?, ?>> buildChain(FactoryKey key) {
        return factories.stream()
                .filter(factory -> factory.matches(key.propsClass(), key.contextClass(), key.inputClass(),
                        key.outputClass(), key.role()))
                .toList();
    }

    private record FactoryKey(Class<?> propsClass, Class<?> inputClass, Class<?> outputClass,
                              Class<?> contextClass, @Nullable ComponentRole role) {
    }
}
