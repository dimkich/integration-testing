package io.github.dimkich.integration.testing.kafka.serde.envelope;

import org.apache.kafka.common.serialization.Serializer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Application-side serializer that writes the binary envelope
 * {@code {VER(1)}{LEN(INT, BE)}{CONTENT}} around a UTF-8 string.
 * <p>
 * Emulates a service that stores enveloped payloads, so the framework side can be
 * checked against real enveloped bytes.
 */
public class EnvelopeStringSerializer implements Serializer<String> {
    private static final byte VERSION = 1;

    /** {@inheritDoc} */
    @Override
    public byte[] serialize(String topic, String data) {
        if (data == null) {
            return null;
        }
        byte[] payload = data.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(1 + Integer.BYTES + payload.length);
        buffer.put(VERSION);
        buffer.putInt(payload.length);
        buffer.put(payload);
        return buffer.array();
    }
}
