package io.github.dimkich.integration.testing.kafka.serde.deserialization;

import org.apache.kafka.common.serialization.Deserializer;

import java.nio.ByteBuffer;

/**
 * Native Kafka {@link Deserializer} for the framework's own sniffer consumer: wraps the
 * {@code byte[]} value of a {@code ConsumerRecord} into a read-ready {@link ByteBuffer} without
 * copying the bytes. This is the only place where the framework's Kafka consumer leaves byte
 * arrays behind.
 */
public class KafkaByteBufferDeserializer implements Deserializer<ByteBuffer> {

    @Override
    public ByteBuffer deserialize(String topic, byte[] data) {
        return data == null ? null : ByteBuffer.wrap(data);
    }
}
