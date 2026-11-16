package io.github.dimkich.integration.testing.storage.diff.model;

/**
 * Resolves the semantic type name written as the {@code type} attribute of a difference element.
 * <p>
 * The name depends on the serializer configuration of the current test format, so the resolver is
 * supplied by the engine. A presenter receives this narrow port instead of the whole difference
 * engine: it asks only for a name and stays independent of the engine.
 */
@FunctionalInterface
public interface TypeNames {

    /**
     * @param value value to inspect, may be {@code null}
     * @return type name, or {@code null} when the value has no explicit type
     */
    String typeOf(Object value);
}
