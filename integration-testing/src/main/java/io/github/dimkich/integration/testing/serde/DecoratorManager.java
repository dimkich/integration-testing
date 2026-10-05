package io.github.dimkich.integration.testing.serde;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Applies decorator factories to an already resolved converter, for example to wrap it into a
 * binary envelope.
 *
 * <p>Decorators are matched by configuration/input/output/context and all applicable ones are
 * applied in registration order; a factory returning {@code null} leaves the current converter
 * unchanged. Chains are cached per request key.
 */
@RequiredArgsConstructor
public class DecoratorManager {

    private final List<TestSerdeDecoratorFactory<?, ?, ?, ?>> decorators;
    private final ConcurrentHashMap<DecoratorKey, List<TestSerdeDecoratorFactory<?, ?, ?, ?>>> chainCache =
            new ConcurrentHashMap<>();

    /**
     * Applies all matching decorators to the converter.
     *
     * @param converter    converter to decorate
     * @param props        serde configuration; decorators are selected by its class
     * @param inputClass   requested input type
     * @param outputClass  requested output type
     * @param contextClass requested context type
     * @return the decorated converter, or the given converter when no decorator matches
     */
    public <I, O, C extends TestSerdeContext> TestSerdeConverter<I, O, C> decorate(
            TestSerdeConverter<I, O, C> converter, TestSerdeProperties props,
            Class<I> inputClass, Class<O> outputClass, Class<C> contextClass) {

        TestSerdeConverter<I, O, C> result = converter;
        for (TestSerdeDecoratorFactory<?, ?, ?, ?> decorator
                : chainFor(new DecoratorKey(props.getClass(), inputClass, outputClass, contextClass))) {
            TestSerdeConverter<I, O, C> decorated = decorator.tryDecorate(result, props);
            if (decorated != null) {
                result = decorated;
            }
        }
        return result;
    }

    private List<TestSerdeDecoratorFactory<?, ?, ?, ?>> chainFor(DecoratorKey key) {
        return chainCache.computeIfAbsent(key, k -> decorators.stream()
                .filter(decorator -> decorator.matches(k.propsClass(), k.contextClass(), k.inputClass(),
                        k.outputClass(), null))
                .toList());
    }

    private record DecoratorKey(Class<?> propsClass, Class<?> inputClass, Class<?> outputClass,
                                Class<?> contextClass) {
    }
}
