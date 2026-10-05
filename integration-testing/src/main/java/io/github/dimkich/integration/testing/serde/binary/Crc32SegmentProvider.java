package io.github.dimkich.integration.testing.serde.binary;

import lombok.Getter;

import java.nio.ByteBuffer;
import java.util.HexFormat;
import java.util.List;
import java.util.zip.CRC32;

/**
 * Provider for the {@code {CRC32}} tag: a fixed 4-byte CRC32 checksum of the envelope bytes,
 * validated on read. Accepts an optional byte order parameter.
 */
@Getter
public class Crc32SegmentProvider implements BinarySegmentProvider {

    private final String name = "CRC32";

    @Override
    public BinarySegment create(List<String> params) {
        BinaryByteOrder order = BinaryByteOrder.fromString(!params.isEmpty() ? params.get(0) : null);
        return new Crc32Segment(order);
    }

    private record Crc32Segment(BinaryByteOrder order) implements BinarySegment {
        private static final HexFormat hexFormat = HexFormat.of().withUpperCase();

        @Override
        public void read(ByteBuffer buffer, ReadContext ctx) {
            int checksumOffset = buffer.position();
            order.apply(buffer);
            int stored = buffer.getInt();
            CRC32 crc = new CRC32();
            ByteBuffer prefix = buffer.duplicate();
            prefix.position(0);
            prefix.limit(checksumOffset);
            crc.update(prefix);
            int computed = (int) crc.getValue();
            if (stored != computed) {
                throw new RuntimeException("CRC32 mismatch. Expected: " + hexFormat.toHexDigits(computed)
                        + ", got: " + hexFormat.toHexDigits(stored));
            }
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
