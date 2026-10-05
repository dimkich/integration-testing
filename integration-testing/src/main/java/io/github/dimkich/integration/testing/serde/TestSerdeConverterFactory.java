package io.github.dimkich.integration.testing.serde;

import org.springframework.lang.Nullable;

/**
 * Factory of {@link TestSerdeConverter}s: creates a converter for a matching request from the
 * component configuration.
 *
 * <p>Implementations return {@code null} when they do not handle the request; {@link #tryCreate}
 * casts the request to the declared types and verifies the created converter.
 *
 * @param <I> input type
 * @param <O> output type
 * @param <C> context type
 * @param <R> role type
 * @param <P> configuration type
 */
public interface TestSerdeConverterFactory<I, O, C extends TestSerdeContext, R extends ComponentRole,
        P extends TestSerdeProperties> extends TestSerdeComponent<I, O, C, R, P> {

    /**
     * Creates a converter for the request.
     *
     * @param config       component configuration
     * @param inputClass   requested input type
     * @param outputClass  requested output type
     * @param contextClass requested context type
     * @param role         requested role, or {@code null}
     * @return the created converter, or {@code null} when the factory does not handle the request
     */
    @Nullable
    TestSerdeConverter<I, O, C> create(P config, Class<I> inputClass, Class<O> outputClass, Class<C> contextClass,
                                       @Nullable R role);

    /**
     * Casts the request to the declared types, delegates to {@link #create} and verifies the result.
     *
     * @param props        component configuration
     * @param inputClass   requested input type
     * @param outputClass  requested output type
     * @param contextClass requested context type
     * @param role         requested role, or {@code null}
     * @param <RI>         requested input type
     * @param <RO>         requested output type
     * @param <RC>         requested context type
     * @return the created converter, or {@code null} when the factory does not handle the request
     * @throws IllegalArgumentException if the created converter does not satisfy the request
     */
    @SuppressWarnings("unchecked")
    @Nullable
    default <RI, RO, RC extends TestSerdeContext> TestSerdeConverter<RI, RO, RC> tryCreate(
            TestSerdeProperties props, Class<RI> inputClass, Class<RO> outputClass, Class<RC> contextClass,
            @Nullable ComponentRole role) {
        TestSerdeConverter<I, O, C> converter = create(getPropertiesClass().cast(props), (Class<I>) inputClass,
                (Class<O>) outputClass, (Class<C>) contextClass, (R) role);
        return verify("Factory", converter, inputClass, outputClass, contextClass);
    }
}
