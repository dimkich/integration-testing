package io.github.dimkich.integration.testing.serde.binary;

import lombok.Getter;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Provider for the {@code {LEN}} tag: a length prefix encoding the payload size. Both the numeric
 * type and the byte order are optional (defaults {@code INT} and {@code BE}).
 */
@Getter
public class LengthSegmentProvider implements BinarySegmentProvider {

    private final String name = "LEN";

    @Override
    public BinarySegment create(List<String> params) {
        BinaryDataType type = BinaryDataType.fromString(!params.isEmpty() ? params.get(0) : "INT");
        BinaryByteOrder order = BinaryByteOrder.fromString(params.size() > 1 ? params.get(1) : null);

        return new LengthSegment(type, order);
    }

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
