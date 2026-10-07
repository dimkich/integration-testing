package io.github.dimkich.integration.testing.serde;

import org.springframework.lang.Nullable;

/**
 * Common contract of serde pipeline components: converter factories, providers, adapters and
 * decorator factories.
 *
 * <p>Describes the signature used to match a resolution request — input, output, context,
 * properties and role — plus ordering ({@link #compare}) and the shared checks
 * ({@link #matches}, {@link #verify}). A {@code null} input or output class means "any type", a
 * {@code null} properties class matches every configuration, and a non-null role restricts the
 * component to requests with the same {@link ComponentRole#name()}.
 *
 * @param <I> input type the component produces converters for
 * @param <O> output type the component produces converters for
 * @param <C> context type
 * @param <R> role type
 * @param <P> configuration type
 */
public interface TestSerdeComponent<I, O, C extends TestSerdeContext, R extends ComponentRole,
        P extends TestSerdeProperties> {

    /**
     * Returns the input type the component accepts.
     *
     * @return the input type, or {@code null} when any type is accepted
     */
    @Nullable
    Class<I> getInputClass();

    /**
     * Returns the output type the component produces.
     *
     * @return the output type, or {@code null} when any type is accepted
     */
    @Nullable
    Class<O> getOutputClass();

    /**
     * Returns the context type the component requires; requests with a subtype of it are accepted.
     *
     * @return the context class
     */
    Class<C> getContextClass();

    /**
     * Returns the configuration class the component is created from.
     *
     * @return the properties class
     */
    Class<P> getPropertiesClass();

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
     * Returns the role the component is restricted to.
     *
     * @return the role, or {@code null} when the component is universal
     */
    @Nullable
    default R getRole() {
        return null;
    }

    /**
     * Checks whether the component handles a request with the given types and role.
     *
     * <p>The input/output pair is matched by the Liskov substitution principle: the declared
     * input must be a supertype of the requested input (contravariance), and the declared output
     * must be a subtype of the requested output (covariance). A {@code null} declared type means
     * "any type". Decorators override this to invariant matching, because a decorator wraps a
     * converter of exactly its declared signature.
     *
     * @param propsClass   configuration class of the request
     * @param contextClass context class of the request
     * @param inputClass   input class of the request
     * @param outputClass  output class of the request
     * @param role         requested role, or {@code null}
     * @return {@code true} when the component matches the request
     */
    default boolean matches(Class<?> propsClass, Class<?> contextClass, Class<?> inputClass, Class<?> outputClass,
                            @Nullable ComponentRole role) {
        return matchesRole(role)
                && getPropertiesClass().isAssignableFrom(propsClass)
                && getContextClass().isAssignableFrom(contextClass)
                && (getInputClass() == null || getInputClass().isAssignableFrom(inputClass))
                && (getOutputClass() == null || outputClass.isAssignableFrom(getOutputClass()));
    }

    /**
     * Orders components so that the more specific one wins: by role, then by the specificity of the
     * properties, input, output and context classes.
     *
     * @param other component to compare with
     * @return a negative value when this component is more specific, a positive value when
     * {@code other} is, {@code 0} when they are equally specific
     */
    default int compare(TestSerdeComponent<?, ?, ?, ?, ?> other) {
        int result = TypeSpecificity.compareRoles(getRole(), other.getRole());
        if (result == 0) {
            result = TypeSpecificity.compare(getPropertiesClass(), other.getPropertiesClass());
        }
        if (result == 0) {
            result = TypeSpecificity.compare(getInputClass(), other.getInputClass());
        }
        if (result == 0) {
            result = TypeSpecificity.compare(getOutputClass(), other.getOutputClass());
        }
        if (result == 0) {
            result = TypeSpecificity.compare(getContextClass(), other.getContextClass());
        }
        return result;
    }

    /**
     * Checks that a created converter satisfies the request and casts it to the requested type
     * parameters.
     *
     * @param kind          component kind used in the error message (e.g. {@code "Factory"})
     * @param converter     converter created by the component, may be {@code null}
     * @param inputClass    requested input type
     * @param outputClass   requested output type
     * @param contextClass  requested context type
     * @param <RI>          requested input type
     * @param <RO>          requested output type
     * @param <RC>          requested context type
     * @return the converter cast to the requested types, or {@code null} when the converter is {@code null}
     * @throws IllegalArgumentException if the converter does not satisfy the request
     */
    @Nullable
    default <RI, RO, RC extends TestSerdeContext> TestSerdeConverter<RI, RO, RC> verify(
            String kind, @Nullable TestSerdeConverter<?, ?, ?> converter,
            Class<RI> inputClass, Class<RO> outputClass, Class<RC> contextClass) {
        if (converter == null) {
            return null;
        }
        if (!converter.satisfies(inputClass, outputClass, contextClass)) {
            throw new IllegalArgumentException(String.format(
                    "%s [%s] returned converter [%s -> %s] which does not satisfy request [%s -> %s] "
                            + "under context [%s]",
                    kind, getClass().getName(),
                    converter.getInputClassName(), converter.getOutputClassName(),
                    inputClass.getName(), outputClass.getName(), contextClass.getName()));
        }
        return TestSerdeConverter.uncheckedCast(converter);
    }

    private boolean matchesRole(@Nullable ComponentRole requestedRole) {
        R role = getRole();
        return role == null || (requestedRole != null && role.name().equals(requestedRole.name()));
    }

    private static String typeName(Class<?> type) {
        return type == null ? "any" : type.getName();
    }
}
