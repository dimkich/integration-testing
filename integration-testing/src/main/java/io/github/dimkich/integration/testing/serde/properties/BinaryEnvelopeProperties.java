package io.github.dimkich.integration.testing.serde.properties;

import io.github.dimkich.integration.testing.serde.SerdeRoleCollector;
import io.github.dimkich.integration.testing.serde.TestSerdeProperties;
import org.springframework.util.StringUtils;

/**
 * Configuration with an optional binary envelope wrapping the resolved serializer/deserializer.
 *
 * @see BinaryEnvelopeSerializerDecorator
 * @see BinaryEnvelopeDeserializerDecorator
 */
public interface BinaryEnvelopeProperties extends TestSerdeProperties {

    /**
     * Returns the binary envelope layout applied around the payload.
     *
     * @return the envelope layout string, may be {@code null}
     */
    String getBinaryEnvelope();

    /**
     * Checks whether a binary envelope is configured.
     *
     * @return {@code true} when the envelope layout is not blank
     */
    default boolean hasBinaryEnvelope() {
        return StringUtils.hasText(getBinaryEnvelope());
    }

    @Override
    default void reportRoles(SerdeRoleCollector collector) {
        if (hasBinaryEnvelope()) {
            collector.addDecorator("binaryEnvelope",
                    "the binary envelope wraps the resolved serializer/deserializer");
        }
    }
}
