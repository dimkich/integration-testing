package io.github.dimkich.integration.testing.redis.codec.segment;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * {@link BinarySegmentProvider} for the {@code STR} tag: encodes fixed or length-prefixed UTF-8
 * string literals in the binary envelope format.
 * <p>
 * Format variants:
 * <ul>
 *   <li>{@code {STR(hex-or-text)}} — fixed byte sequence from UTF-8; creates a
 *       {@link StaticHexSegment} that reads/writes the bytes as-is.</li>
 *   <li>{@code {STR(hex-or-text, type, order)}} — length-prefixed string; creates a segment that
 *       writes a numeric length (using {@link BinaryDataType}) and byte order (via
 *       {@link BinaryByteOrder}), followed by the string bytes.</li>
 * </ul>
 * Example: {@code {STR("hello", INT, BE)}} encodes a 4-byte big-endian length plus the UTF-8
 * bytes of "hello".
 *
 * @see BinarySegmentProvider
 * @see BinaryFormatParser
 * @see StaticHexSegment
 * @see BinaryDataType
 * @see BinaryByteOrder
 */
public class StringSegmentProvider implements BinarySegmentProvider {
    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "STR";
    }

    /** {@inheritDoc} */
    @Override
    public BinarySegment create(List<String> params) {
        byte[] stringBytes = params.get(0).getBytes(StandardCharsets.UTF_8);
        if (params.size() > 1) {
            BinaryDataType type = BinaryDataType.fromString(params.get(1));
            BinaryByteOrder order = BinaryByteOrder.fromString(params.get(2));
            return new DynamicStringSegment(stringBytes, type, order);
        }
        return new StaticHexSegment(stringBytes);
    }

    /**
     * Length-prefixed string segment: writes a numeric length (type + order) followed by UTF-8 bytes;
     * on read, advances past the length field and the string bytes.
     */
    private record DynamicStringSegment(byte[] bytes, BinaryDataType type,
                                        BinaryByteOrder order) implements BinarySegment {
        /** {@inheritDoc} Skips length field and string bytes. */
        @Override
        public void read(ByteBuffer buffer, ReadContext ctx) {
            order.apply(buffer);
            int stringLen = type.read(buffer).intValue();
            buffer.position(buffer.position() + stringLen);
        }

        /** {@inheritDoc} Writes length (type + order) followed by string bytes; payload is ignored. */
        @Override
        public void write(ByteBuffer buffer, byte[] payload) {
            order.apply(buffer);
            type.write(buffer, bytes.length);
            buffer.put(bytes);
        }

        /** {@inheritDoc} Returns type size + string byte length. */
        @Override
        public int length() {
            return type.getSize() + bytes.length;
        }
    }
}
