package io.github.dimkich.integration.testing.serde.binary;

import lombok.Getter;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.List;

/**
 * Provider for the {@code {TS}} tag: the current epoch millis, skipped on read. The numeric type
 * and byte order are required.
 */
@Getter
public class TsSegmentProvider implements BinarySegmentProvider {

    private final String name = "TS";

    @Override
    public BinarySegment create(List<String> params) {
        BinaryDataType type = BinaryDataType.fromString(params.get(0));
        BinaryByteOrder order = BinaryByteOrder.fromString(params.get(1));
        return new TsSegment(type, order);
    }

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
