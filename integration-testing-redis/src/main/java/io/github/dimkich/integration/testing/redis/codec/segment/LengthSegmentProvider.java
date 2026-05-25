package io.github.dimkich.integration.testing.redis.codec.segment;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Provider for the {@code {LEN}} format tag in a binary envelope layout.
 * <p>
 * Produces a length-prefix segment that encodes the payload size as a numeric value. When reading,
 * the segment consumes the length field and sets {@link ReadContext#setPayloadLength(int)} so that
 * a subsequent {@link ContentSegmentProvider} segment knows how many bytes to read. When writing,
 * it emits the payload length using the configured numeric type and byte order.
 * <p>
 * Parameters:
 * <ul>
 *   <li>{@code type} (optional) — {@link BinaryDataType} for the length value (e.g. {@code INT}, {@code LONG},
 *       {@code SHORT}, {@code BYTE}); defaults to {@code INT} if omitted</li>
 *   <li>{@code order} (optional) — {@link BinaryByteOrder} ({@code BE}, {@code LE}); defaults to
 *       {@link BinaryByteOrder#BIG_ENDIAN} if omitted</li>
 * </ul>
 * <p>
 * Example: {@code {LEN(INT, BE)}} creates a 4-byte big-endian length prefix.
 *
 * @see BinarySegmentProvider
 * @see BinaryFormatParser
 * @see ContentSegmentProvider
 */
public class LengthSegmentProvider implements BinarySegmentProvider {
    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "LEN";
    }

    /**
     * {@inheritDoc}
     * @param params first element is type (optional, default INT), second is byte order (optional, default BE)
     */
    @Override
    public BinarySegment create(List<String> params) {
        BinaryDataType type = BinaryDataType.fromString(!params.isEmpty() ? params.get(0) : "INT");
        BinaryByteOrder order = BinaryByteOrder.fromString(params.size() > 1 ? params.get(1) : null);

        return new LengthSegment(type, order);
    }

    /** Length-prefix segment that reads/writes payload size using a numeric type and byte order. */
    private record LengthSegment(BinaryDataType type, BinaryByteOrder order) implements BinarySegment {
        @Override
        public void read(ByteBuffer buffer, ReadContext ctx) {
            order.apply(buffer);
            ctx.setPayloadLength(type.read(buffer).intValue());
        }

        @Override
        public void write(ByteBuffer buffer, byte[] payload) {
            order.apply(buffer);
            type.write(buffer, payload.length);
        }

        @Override
        public int length() {
            return type.getSize();
        }
    }
}
