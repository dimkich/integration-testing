package io.github.dimkich.integration.testing.kafka.serde.envelope;

import org.apache.kafka.common.serialization.Deserializer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Application-side deserializer for the binary envelope
 * {@code {VER(1)}{LEN(INT, BE)}{CONTENT}} around a UTF-8 string.
 * <p>
 * The version byte and the declared length are validated strictly, so a test fails
 * loudly when the framework writes or expects bytes without the envelope.
 */
public class EnvelopeStringDeserializer implements Deserializer<String> {
    private static final byte VERSION = 1;

    /** {@inheritDoc} */
    @Override
    public String deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.wrap(data);
        byte version = buffer.get();
        if (version != VERSION) {
            throw new IllegalArgumentException(String.format(
                    "Unexpected envelope version %d (expected %d) on topic [%s]", version, VERSION, topic));
        }
        int length = buffer.getInt();
        if (length != buffer.remaining()) {
            throw new IllegalArgumentException(String.format(
                    "Envelope length %d does not match payload length %d on topic [%s]",
                    length, buffer.remaining(), topic));
        }
        byte[] payload = new byte[length];
        buffer.get(payload);
        return new String(payload, StandardCharsets.UTF_8);
    }
}
