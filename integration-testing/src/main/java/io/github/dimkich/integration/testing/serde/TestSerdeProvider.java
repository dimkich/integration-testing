package io.github.dimkich.integration.testing.serde;

/**
 * Format provider — a factory for a (serializer, deserializer) pair for a
 * specific data representation: JSON, XML, YAML, string, bytes, ...
 *
 * @param <P> config class this provider can read
 */
public interface TestSerdeProvider<P extends SerdeProperties> {

    /**
     * Returns the format name used to select this provider in configuration
     * (for example {@code json}, {@code xml}, {@code string}).
     *
     * @return the provider name
     */
    String getName();

    /**
     * Creates a serializer for the given configuration.
     *
     * @param config provider configuration; the concrete type is declared by the
     *               provider's generic parameter
     * @return a new serializer instance
     */
    Object createSerializer(P config);

    /**
     * Creates a deserializer for the given configuration.
     *
     * @param config provider configuration; the concrete type is declared by the
     *               provider's generic parameter
     * @return a new deserializer instance
     */
    Object createDeserializer(P config);
}
