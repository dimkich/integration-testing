package io.github.dimkich.integration.testing.redis.codec.segment;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.List;

/**
 * Provider for the {@code {TS}} format tag in a binary envelope layout.
 * <p>
 * Produces a timestamp segment that encodes a millisecond epoch value. When reading, the segment
 * skips the specified number of bytes without using the value. When writing, it emits the current
 * time ({@link Instant#now()}) as milliseconds since epoch using the configured numeric type and
 * byte order.
 * <p>
 * Parameters:
 * <ul>
 *   <li>{@code type} (required) — {@link BinaryDataType} for the timestamp (e.g. {@code LONG}, {@code INT})</li>
 *   <li>{@code order} (required) — {@link BinaryByteOrder} ({@code BE}, {@code LE})</li>
 * </ul>
 * <p>
 * Example: {@code {TS(LONG, BE)}} creates an 8-byte big-endian timestamp field.
 *
 * @see BinarySegmentProvider
 * @see BinaryFormatParser
 */
public class TsSegmentProvider implements BinarySegmentProvider {
    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "TS";
    }

    /**
     * {@inheritDoc}
     * @param params first element is type (required), second is byte order (required)
     */
    @Override
    public BinarySegment create(List<String> params) {
        BinaryDataType type = BinaryDataType.fromString(params.get(0));
        BinaryByteOrder order = BinaryByteOrder.fromString(params.get(1));
        return new TsSegment(type, order);
    }

    /** Timestamp segment that skips on read and writes current epoch millis on write. */
    private record TsSegment(BinaryDataType type, BinaryByteOrder order) implements BinarySegment {
        @Override
        public void read(ByteBuffer buffer, ReadContext ctx) {
            buffer.position(buffer.position() + type.getSize());
        }

        @Override
        public void write(ByteBuffer buffer, byte[] payload) {
            order.apply(buffer);
            type.write(buffer, Instant.now().toEpochMilli());
        }

        @Override
        public int length() {
            return type.getSize();
        }
    }
}
