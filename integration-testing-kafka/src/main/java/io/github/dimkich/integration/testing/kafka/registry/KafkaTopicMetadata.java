package io.github.dimkich.integration.testing.kafka.registry;

import io.github.dimkich.integration.testing.kafka.serde.deserialization.KafkaRecordDeserializer;
import io.github.dimkich.integration.testing.kafka.serde.serialization.KafkaRecordSerializer;
import io.github.dimkich.integration.testing.storage.exclusion.FieldExclusionTree;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.Supplier;

/**
 * Lazily initialized serde and filtering metadata of a single topic: record
 * serializer/deserializer, ignore flags and the compiled excluded-fields tree.
 */
@RequiredArgsConstructor
public class KafkaTopicMetadata {
    private final Supplier<KafkaRecordDeserializer> deserializerSupplier;
    private final Supplier<KafkaRecordSerializer> serializerSupplier;
    /** Returns whether the topic is ignored completely: its messages are neither sent nor captured. */
    @Getter
    private final boolean ignore;
    /** Returns whether messages sent by the test itself are excluded from the captured inbound messages. */
    @Getter
    private final boolean ignoreInbound;
    /** Returns the compiled tree of fields excluded from comparison. */
    @Getter
    private final FieldExclusionTree excludedFields;
    private volatile KafkaRecordDeserializer deserializer;
    private volatile KafkaRecordSerializer serializer;

    /**
     * Returns the record serializer, creating it on first access.
     *
     * @return the record serializer
     */
    public KafkaRecordSerializer getSerializer() {
        if (serializer == null) {
            synchronized (this) {
                if (serializer == null) {
                    serializer = serializerSupplier.get();
                }
            }
        }
        return serializer;
    }

    /**
     * Returns the record deserializer, creating it on first access.
     *
     * @return the record deserializer
     */
    public KafkaRecordDeserializer getDeserializer() {
        if (deserializer == null) {
            synchronized (this) {
                if (deserializer == null) {
                    deserializer = deserializerSupplier.get();
                }
            }
        }
        return deserializer;
    }
}
