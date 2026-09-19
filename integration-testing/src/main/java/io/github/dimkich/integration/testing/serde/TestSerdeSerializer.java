package io.github.dimkich.integration.testing.serde;

/**
 * Serializes a value {@code T} to bytes: {@code T -> byte[]}.
 *
 * <p>The context parameter {@code C} carries transport metadata (topic,
 * headers, etc.). Core providers receive {@link SerdeContext} — the
 * marker with no methods — and ignore it. Platform providers receive
 * a richer context (e.g. {@code KafkaSerdeContext}).
 *
 * @param <T> data type
 * @param <C> context type; must extend {@link SerdeContext}
 */
public interface TestSerdeSerializer<T, C extends SerdeContext> {
    /**
     * Returns the context type this serializer accepts; used by adapters to verify
     * context compatibility at runtime.
     *
     * @return the declared context class
     */
    Class<C> getContextClass();

    /**
     * Serializes the given value to bytes.
     *
     * @param data the value to serialize, may be {@code null}
     * @param context transport context; core implementations ignore it
     * @return the serialized representation, or {@code null} if {@code data} is {@code null}
     */
    byte[] serialize(T data, C context);

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
