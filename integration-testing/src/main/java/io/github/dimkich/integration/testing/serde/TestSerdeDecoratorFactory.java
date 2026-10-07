package io.github.dimkich.integration.testing.serde;

import org.springframework.lang.Nullable;

/**
 * Factory of converter decorators: wraps an already resolved {@link TestSerdeConverter}, for
 * example to apply a binary envelope.
 *
 * <p>Implementations return {@code null} when they do not apply to the given configuration;
 * {@link #tryDecorate} casts the converter and the configuration to the declared types.
 *
 * @param <I> input type
 * @param <O> output type
 * @param <C> context type
 * @param <P> configuration type
 */
public interface TestSerdeDecoratorFactory<I, O, C extends TestSerdeContext, P extends TestSerdeProperties>
        extends TestSerdeComponent<I, O, C, ComponentRole, P> {

    /**
     * Wraps the converter.
     *
     * @param converter converter to decorate
     * @param config    component configuration
     * @return the decorated converter, or {@code null} when the decorator does not apply
     */
    @Nullable
    TestSerdeConverter<I, O, C> decorate(TestSerdeConverter<I, O, C> converter, P config);

    /**
     * Checks whether the decorator applies to a request. Decorators are invariant: a decorator
     * wraps a converter of exactly the declared signature, so the requested input/output pair must
     * be equal to the declared one ({@code null} means "any type"). This narrows the polymorphic
     * matching of {@link TestSerdeComponent#matches}.
     *
     * @param propsClass   configuration class of the request
     * @param contextClass context class of the request
     * @param inputClass   input class of the request
     * @param outputClass  output class of the request
     * @param role         requested role, or {@code null}
     * @return {@code true} when the decorator matches the request
     */
    @Override
    default boolean matches(Class<?> propsClass, Class<?> contextClass, Class<?> inputClass, Class<?> outputClass,
                            @Nullable ComponentRole role) {
        return TestSerdeComponent.super.matches(propsClass, contextClass, inputClass, outputClass, role)
                && (getInputClass() == null || getInputClass().equals(inputClass))
                && (getOutputClass() == null || getOutputClass().equals(outputClass));
    }

    /**
     * Casts the converter and the configuration to the declared types and delegates to
     * {@link #decorate}.
     *
     * @param converter converter to decorate
     * @param props     component configuration
     * @param <RI>      requested input type
     * @param <RO>      requested output type
     * @param <RC>      requested context type
     * @return the decorated converter, or {@code null} when the decorator does not apply
     */
    @SuppressWarnings("unchecked")
    @Nullable
    default <RI, RO, RC extends TestSerdeContext> TestSerdeConverter<RI, RO, RC> tryDecorate(
            TestSerdeConverter<RI, RO, RC> converter, TestSerdeProperties props) {
        return TestSerdeConverter.uncheckedCast(decorate((TestSerdeConverter<I, O, C>) converter,
                getPropertiesClass().cast(props)));
    }
}
