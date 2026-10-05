package io.github.dimkich.integration.testing.serde.binary;

import lombok.Getter;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Provider for the {@code {FIX}} tag: a fixed-size segment of filler bytes ({@code size} and an
 * optional {@code value}, default {@code 0}).
 */
@Getter
public class FixSegmentProvider implements BinarySegmentProvider {

    private final String name = "FIX";

    @Override
    public BinarySegment create(List<String> params) {
        int size = Integer.parseInt(params.get(0));
        byte value = (byte) (params.size() > 1 ? Integer.decode(params.get(1)) : 0);
        return new FixSegment(size, value);
    }

    private record FixSegment(int size, byte value) implements BinarySegment {
        @Override
        public void read(ByteBuffer buffer, ReadContext ctx) {
            buffer.position(buffer.position() + size);
        }

        @Override
        public void write(ByteBuffer buffer, byte[] payload) {
            for (int i = 0; i < size; i++) {
                buffer.put(value);
            }
        }

        @Override
        public int length() {
            return size;
        }
    }
}
