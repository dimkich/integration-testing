package io.github.dimkich.integration.testing.serde;

/**
 * Marker interface for serialization/deserialization context.
 * <p>
 * Platform modules extend this interface and add typed accessors
 * (e.g. {@code topic}, {@code Headers} for Kafka). Core providers
 * work with the marker and are transport-agnostic.
 */
public interface TestSerdeContext {
    TestSerdeContext EMPTY = new TestSerdeContext() {
    };
}
