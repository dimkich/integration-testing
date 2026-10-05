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
