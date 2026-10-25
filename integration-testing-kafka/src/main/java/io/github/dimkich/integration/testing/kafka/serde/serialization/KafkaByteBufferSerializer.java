package io.github.dimkich.integration.testing.kafka.serde.serialization;

import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import org.apache.kafka.common.serialization.Serializer;

import java.nio.ByteBuffer;

/**
 * Native Kafka {@link Serializer} for the framework's own producer: converts the
 * {@link ByteBuffer} value of a {@code ProducerRecord} to the {@code byte[]} required by the
 * Kafka client protocol. This is the only place where the framework's Kafka producer returns to
 * byte arrays.
 */
public class KafkaByteBufferSerializer implements Serializer<ByteBuffer> {

    @Override
    public byte[] serialize(String topic, ByteBuffer data) {
        return TestSerdeConverter.toByteArray(data);
    }
}
