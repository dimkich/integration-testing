package io.github.dimkich.integration.testing.serde;

/**
 * Deserializes bytes to a value {@code T}: {@code byte[] -> T}.
 *
 * @param <T> data type
 * @param <C> context type; must extend {@link SerdeContext}
 */
public interface TestSerdeDeserializer<T, C extends SerdeContext> {
    /**
     * Returns the context type this deserializer accepts; used by adapters to verify
     * context compatibility at runtime.
     *
     * @return the declared context class
     */
    Class<C> getContextClass();

    /**
     * Deserializes bytes to a value.
     *
     * @param data the serialized bytes, may be {@code null}
     * @param context transport context; core implementations ignore it
     * @return the deserialized value, or {@code null} if {@code data} is {@code null}
     */
    T deserialize(byte[] data, C context);

    /**
     * Verifies that the context class supplied by a caller is compatible with the
     * context type declared by {@link #getContextClass()}.
     *
     * @param suppliedContext the context class supplied by an adapter
     * @param remediationHint message appended to the error, explaining how to fix the mismatch
     * @throws IllegalArgumentException if {@code suppliedContext} is not assignable
     *                                  to the declared context type
     */
    default void assertAcceptsContext(Class<? extends SerdeContext> suppliedContext,
                                      String remediationHint) {
        Class<C> declared = getContextClass();
        if (declared.isAssignableFrom(suppliedContext)) {
            return;
        }
        throw new IllegalArgumentException(String.format(
                "Provider [%s] declares context type [%s], but this adapter supplies [%s]. %s",
                getClass().getName(),
                declared.getName(),
                suppliedContext.getName(),
                remediationHint));
    }
}
