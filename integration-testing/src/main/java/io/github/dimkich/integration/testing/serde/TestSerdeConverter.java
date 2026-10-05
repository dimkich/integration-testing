package io.github.dimkich.integration.testing.serde;

import org.springframework.lang.Nullable;

import java.util.function.BiFunction;

/**
 * A resolved conversion step of the serde pipeline: converts an input of a known type into an
 * output of a known type within a typed context.
 *
 * <p>Converters are independent of how they were obtained (provider, converter factory, adapter or
 * decorator) and are validated against the request by {@link #satisfies}.
 *
 * @param <I> input type
 * @param <O> output type
 * @param <C> context type
 */
public interface TestSerdeConverter<I, O, C extends TestSerdeContext> {

    /**
     * Returns the input type of the converter.
     *
     * @return the input type, or {@code null} when any type is accepted
     */
    @Nullable
    Class<I> getInputClass();

    /**
     * Returns the output type of the converter.
     *
     * @return the output type, or {@code null} when any type is accepted
     */
    @Nullable
    Class<O> getOutputClass();

    /**
     * Returns the context type of the converter.
     *
     * @return the context class
     */
    Class<C> getContextClass();

    /**
     * Returns the input class name for messages.
     *
     * @return the input class name, or {@code "any"} when the input type is {@code null}
     */
    default String getInputClassName() {
        return typeName(getInputClass());
    }

    /**
     * Returns the output class name for messages.
     *
     * @return the output class name, or {@code "any"} when the output type is {@code null}
     */
    default String getOutputClassName() {
        return typeName(getOutputClass());
    }

    /**
     * Converts the input into the output.
     *
     * @param input   value to convert, may be {@code null}
     * @param context transport context of the conversion
     * @return the converted value
     */
    O convert(I input, C context);

    /**
     * Checks whether the converter matches the requested types: the context must be assignable to
     * {@link #getContextClass()}, and declared input/output types must be equal to the requested ones.
     *
     * @param inputClass   requested input type
     * @param outputClass  requested output type
     * @param contextClass requested context type
     * @return {@code true} when the converter satisfies the request
     */
    default boolean satisfies(Class<?> inputClass, Class<?> outputClass, Class<?> contextClass) {
        return getContextClass().isAssignableFrom(contextClass)
                && (getInputClass() == null || getInputClass().equals(inputClass))
                && (getOutputClass() == null || getOutputClass().equals(outputClass));
    }

    /**
     * Casts a converter to the requested type parameters; type safety is enforced separately by
     * {@link #satisfies}.
     *
     * @param converter converter to cast
     * @param <I>       requested input type
     * @param <O>       requested output type
     * @param <C>       requested context type
     * @return the same converter with the requested type parameters
     */
    @SuppressWarnings("unchecked")
    static <I, O, C extends TestSerdeContext> TestSerdeConverter<I, O, C> uncheckedCast(
            TestSerdeConverter<?, ?, ?> converter) {
        return (TestSerdeConverter<I, O, C>) converter;
    }

    /**
     * Creates a converter from a conversion function.
     *
     * @param inputClass   input type of the converter, or {@code null} for any
     * @param outputClass  output type of the converter, or {@code null} for any
     * @param contextClass context type of the converter
     * @param function     conversion function
     * @param <I>          input type
     * @param <O>          output type
     * @param <C>          context type
     * @return a converter delegating to the function
     */
    static <I, O, C extends TestSerdeContext> TestSerdeConverter<I, O, C> of(
            @Nullable Class<I> inputClass, @Nullable Class<O> outputClass, Class<C> contextClass,
            BiFunction<I, C, O> function) {
        return new TestSerdeConverter<>() {
            @Override
            public Class<I> getInputClass() {
                return inputClass;
            }

            @Override
            public Class<O> getOutputClass() {
                return outputClass;
            }

            @Override
            public Class<C> getContextClass() {
                return contextClass;
            }

            @Override
            public O convert(I input, C context) {
                return function.apply(input, context);
            }
        };
    }

    private static String typeName(Class<?> type) {
        return type == null ? "any" : type.getName();
    }
}
