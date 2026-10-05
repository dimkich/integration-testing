package io.github.dimkich.integration.testing.kafka.registry;

import io.github.dimkich.integration.testing.kafka.KafkaRecord;
import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import io.github.dimkich.integration.testing.storage.exclusion.FieldExclusionTree;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.function.Supplier;

/**
 * Lazily initialized serde and filtering metadata of a single topic: record
 * serializer/deserializer converters, ignore flags and the compiled excluded-fields tree.
 */
@RequiredArgsConstructor
@SuppressWarnings("rawtypes")
public class KafkaTopicMetadata {
    private final Supplier<TestSerdeConverter<ConsumerRecord, KafkaRecord, TestSerdeContext>> deserializerSupplier;
    private final Supplier<TestSerdeConverter<KafkaRecord, ProducerRecord, TestSerdeContext>> serializerSupplier;
    /** Returns whether the topic is ignored completely: its messages are neither sent nor captured. */
    @Getter
    private final boolean ignore;
    /** Returns whether messages sent by the test itself are excluded from the captured inbound messages. */
    @Getter
    private final boolean ignoreInbound;
    /** Returns the compiled tree of fields excluded from comparison. */
    @Getter
    private final FieldExclusionTree excludedFields;
    private volatile TestSerdeConverter<ConsumerRecord, KafkaRecord, TestSerdeContext> deserializer;
    private volatile TestSerdeConverter<KafkaRecord, ProducerRecord, TestSerdeContext> serializer;

    /**
     * Returns the record serializer converter, creating it on first access.
     *
     * @return the record serializer converter
     */
    public TestSerdeConverter<KafkaRecord, ProducerRecord, TestSerdeContext> getSerializer() {
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
     * Returns the record deserializer converter, creating it on first access.
     *
     * @return the record deserializer converter
     */
    public TestSerdeConverter<ConsumerRecord, KafkaRecord, TestSerdeContext> getDeserializer() {
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
