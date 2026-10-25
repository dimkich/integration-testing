package io.github.dimkich.integration.testing.serde.binary;

import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class BinaryEnvelopeTest {

    private final BinaryEnvelope envelope = new BinaryEnvelope(new BinaryEnvelopeParser(
            List.of(new LengthSegmentProvider(), new ContentSegmentProvider())).parse("{LEN(INT, BE)}{CONTENT}"));

    @Test
    void wrapAndUnwrapUseOnlyRemainingBytes() {
        ByteBuffer payload = ByteBuffer.wrap(new byte[]{0, 1, 2, 3});
        payload.position(1);

        ByteBuffer wrapped = envelope.wrap(payload);

        assertEquals(1, payload.position());
        assertEquals(4, payload.limit());
        assertEquals(0, wrapped.position());

        ByteBuffer unwrapped = envelope.unwrap(wrapped);

        assertEquals(0, wrapped.position());
        assertEquals(0, unwrapped.position());
        assertArrayEquals(new byte[]{1, 2, 3}, TestSerdeConverter.toByteArray(unwrapped));
    }

    @Test
    void unwrapReturnsZeroCopyView() {
        ByteBuffer wrapped = envelope.wrap(TestSerdeConverter.wrap(new byte[]{1, 2, 3}));

        ByteBuffer payload = envelope.unwrap(wrapped);

        assertSame(wrapped.array(), payload.array());
    }

    @Test
    void nullPayloadStaysNull() {
        assertNull(envelope.wrap(null));
        assertNull(envelope.unwrap(null));
    }
}
