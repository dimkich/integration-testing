package io.github.dimkich.integration.testing.redis.codec.segment;

import lombok.RequiredArgsConstructor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Represents byte order for binary format parsing of Redis data.
 * <p>
 * Redis data can be produced on systems with different native byte orders.
 * This enum allows specifying the byte order when decoding binary segments.
 *
 * @see ByteOrder
 */
@RequiredArgsConstructor
public enum BinaryByteOrder {
    /** Most significant byte first (network byte order). */
    BIG_ENDIAN(ByteOrder.BIG_ENDIAN),

    /** Least significant byte first (typical for x86). */
    LITTLE_ENDIAN(ByteOrder.LITTLE_ENDIAN);

    private final ByteOrder byteOrder;

    /**
     * Applies this byte order to the given buffer.
     *
     * @param buffer the buffer to configure; must not be null
     */
    public void apply(ByteBuffer buffer) {
        buffer.order(byteOrder);
    }

    /**
     * Parses a string into a {@code BinaryByteOrder}.
     * <p>
     * Accepted values: {@code "BE"}, {@code "BIG_ENDIAN"}, {@code "LE"}, {@code "LITTLE_ENDIAN"}.
     * Comparison is case-insensitive.
     *
     * @param order the string representation of byte order; may be null (returns {@link #BIG_ENDIAN})
     * @return the corresponding {@code BinaryByteOrder}
     * @throws IllegalArgumentException if the string is not a recognized byte order
     */
    public static BinaryByteOrder fromString(String order) {
        if (order == null) {
            return BIG_ENDIAN;
        }
        return switch (order.toUpperCase()) {
            case "BE", "BIG_ENDIAN" -> BIG_ENDIAN;
            case "LE", "LITTLE_ENDIAN" -> LITTLE_ENDIAN;
            default -> throw new IllegalArgumentException("Unknown byte order: " + order);
        };
    }
}
