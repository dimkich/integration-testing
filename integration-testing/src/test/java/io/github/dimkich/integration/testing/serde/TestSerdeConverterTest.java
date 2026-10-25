package io.github.dimkich.integration.testing.serde;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class TestSerdeConverterTest {

    @Test
    void wrapReturnsNullForNullArray() {
        assertNull(TestSerdeConverter.wrap(null));
    }

    @Test
    void wrapUsesTheSameArrayWithoutCopying() {
        byte[] bytes = {1, 2, 3};

        ByteBuffer buffer = TestSerdeConverter.wrap(bytes);

        assertEquals(0, buffer.position());
        assertEquals(3, buffer.limit());
        assertSame(bytes, buffer.array());
    }

    @Test
    void toByteArrayReturnsNullForNullBuffer() {
        assertNull(TestSerdeConverter.toByteArray(null));
    }

    @Test
    void toByteArrayCopiesRemainingBytesAndKeepsPosition() {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[]{0, 1, 2, 3, 4});
        buffer.position(1);
        buffer.limit(4);

        byte[] bytes = TestSerdeConverter.toByteArray(buffer);

        assertArrayEquals(new byte[]{1, 2, 3}, bytes);
        assertEquals(1, buffer.position());
        assertEquals(4, buffer.limit());
    }

    @Test
    void toByteArrayReturnsDefensiveCopy() {
        ByteBuffer buffer = TestSerdeConverter.wrap(new byte[]{1, 2, 3});

        byte[] bytes = TestSerdeConverter.toByteArray(buffer);
        bytes[0] = 42;

        assertEquals(1, buffer.get(0));
        assertNotSame(buffer.array(), bytes);
    }
}
