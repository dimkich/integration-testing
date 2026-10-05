package io.github.dimkich.integration.testing.serde;

import org.springframework.lang.Nullable;

/**
 * Bridge between a raw serde source and a {@link TestSerdeConverter} of the requested types.
 *
 * <p>Adapters are needed when a provider returns one type while the module requires another; an
 * adapter is selected by its source class and configuration class (see {@link AdapterManager}).
 * {@link #tryAdapt} casts the source and the configuration to the declared types and verifies the
 * created converter.
 *
 * @param <S> source type produced by a provider or a previous adapter
 * @param <I> input type of the resulting converter
 * @param <O> output type of the resulting converter
 * @param <C> context type
 * @param <R> role type
 * @param <P> configuration type
 */
public interface TestSerdeAdapter<S, I, O, C extends TestSerdeContext, R extends ComponentRole,
        P extends TestSerdeProperties> extends TestSerdeComponent<I, O, C, R, P> {

    /**
     * Returns the source type the adapter accepts; subclasses are accepted as well.
     *
     * @return the source class
     */
    Class<S> getSourceClass();

    /**
     * Adapts the source to a converter of the requested types.
     *
     * @param source       object returned by a provider or a previous adapter
     * @param properties   serde configuration
     * @param inputClass   requested input type
     * @param outputClass  requested output type
     * @param contextClass requested context type
     * @param role         requested role, or {@code null}
     * @return the adapted converter, or {@code null} when the adapter does not handle the source
     */
    @Nullable
    TestSerdeConverter<I, O, C> adapt(S source, P properties, Class<I> inputClass, Class<O> outputClass,
                                      Class<C> contextClass, @Nullable R role);

    /**
     * Casts the source and configuration to the declared types, delegates to {@link #adapt} and
     * verifies the result.
     *
     * @param source       object returned by a provider or a previous adapter
     * @param props        serde configuration
     * @param inputClass   requested input type
     * @param outputClass  requested output type
     * @param contextClass requested context type
     * @param role         requested role, or {@code null}
     * @param <RI>         requested input type
     * @param <RO>         requested output type
     * @param <RC>         requested context type
     * @return the adapted converter, or {@code null} when the adapter does not handle the source
     * @throws IllegalArgumentException if the adapted converter does not satisfy the request
     */
    @SuppressWarnings("unchecked")
    @Nullable
    default <RI, RO, RC extends TestSerdeContext> TestSerdeConverter<RI, RO, RC> tryAdapt(
            Object source, TestSerdeProperties props, Class<RI> inputClass, Class<RO> outputClass,
            Class<RC> contextClass, @Nullable ComponentRole role) {
        TestSerdeConverter<I, O, C> converter = adapt((S) source, getPropertiesClass().cast(props),
                (Class<I>) inputClass, (Class<O>) outputClass, (Class<C>) contextClass, (R) role);
        return verify("Adapter", converter, inputClass, outputClass, contextClass);
    }

    /**
     * Additionally to {@link TestSerdeComponent#matches}, requires the source class to be
     * assignable to {@link #getSourceClass()}.
     *
     * @param sourceClass  class of the source to adapt
     * @param propsClass   configuration class of the request
     * @param contextClass context class of the request
     * @param inputClass   input class of the request
     * @param outputClass  output class of the request
     * @param role         requested role, or {@code null}
     * @return {@code true} when the adapter matches the source and the request
     */
    default boolean matches(Class<?> sourceClass, Class<?> propsClass, Class<?> contextClass, Class<?> inputClass,
                            Class<?> outputClass, @Nullable ComponentRole role) {
        return getSourceClass().isAssignableFrom(sourceClass)
                && TestSerdeComponent.super.matches(propsClass, contextClass, inputClass, outputClass, role);
    }

    /**
     * Orders adapters by role, then by source specificity (a subtype before its ancestors), then as
     * {@link TestSerdeComponent#compare}.
     *
     * @param other adapter to compare with
     * @return a negative value when this adapter is more specific, a positive value when
     * {@code other} is, {@code 0} when they are equally specific
     */
    default int compare(TestSerdeAdapter<?, ?, ?, ?, ?, ?> other) {
        int result = TypeSpecificity.compareRoles(getRole(), other.getRole());
        if (result == 0) {
            result = TypeSpecificity.compareSources(getSourceClass(), other.getSourceClass());
        }
        if (result == 0) {
            result = TestSerdeComponent.super.compare(other);
        }
        return result;
    }
}
