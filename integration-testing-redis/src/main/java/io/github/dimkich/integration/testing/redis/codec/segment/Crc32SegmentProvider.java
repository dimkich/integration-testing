package io.github.dimkich.integration.testing.redis.codec.segment;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.zip.CRC32;

/**
 * Provider for the {@code {CRC32}} format tag in a binary envelope layout.
 * <p>
 * Produces a fixed-length segment (4 bytes) that reads and writes a CRC32 checksum of the
 * envelope content. When writing, the checksum is computed over all bytes currently in the
 * buffer and appended. When reading, the segment consumes the 4-byte checksum from the buffer.
 * <p>
 * Accepts an optional byte order parameter: {@code {CRC32}} or {@code {CRC32(BE)}} for
 * big-endian (default), {@code {CRC32(LE)}} for little-endian.
 *
 * @see BinarySegmentProvider
 * @see BinaryByteOrder
 */
public class Crc32SegmentProvider implements BinarySegmentProvider {
    @Override
    public String getName() {
        return "CRC32";
    }

    @Override
    public BinarySegment create(List<String> params) {
        BinaryByteOrder order = BinaryByteOrder.fromString(!params.isEmpty() ? params.get(0) : null);
        return new Crc32Segment(order);
    }

    /**
     * Fixed-length (4-byte) segment that reads and writes a CRC32 checksum.
     * Checksum covers all bytes in the buffer when writing.
     */
    private record Crc32Segment(BinaryByteOrder order) implements BinarySegment {
        @Override
        public void read(ByteBuffer buffer, ReadContext ctx) {
            order.apply(buffer);
            buffer.getInt();
        }

        @Override
        public void write(ByteBuffer buffer, byte[] payload) {
            CRC32 crc = new CRC32();
            ByteBuffer readBuffer = buffer.duplicate();
            readBuffer.flip();
            crc.update(readBuffer);
            order.apply(buffer);
            buffer.putInt((int) crc.getValue());
        }

        @Override
        public int length() {
            return 4;
        }
    }
}
