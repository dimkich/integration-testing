package io.github.dimkich.integration.testing.serde.binary;

import lombok.Getter;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Provider for the {@code {VER}} tag: a single version byte, validated on read.
 */
@Getter
public class VersionSegmentProvider implements BinarySegmentProvider {

    private final String name = "VER";

    @Override
    public BinarySegment create(List<String> params) {
        byte version = (byte) Integer.decode(params.get(0)).intValue();
        return new VersionSegment(version);
    }

    private record VersionSegment(byte version) implements BinarySegment {
        @Override
        public void read(ByteBuffer buffer, ReadContext ctx) {
            byte v = buffer.get();
            if (v != version) {
                throw new RuntimeException("Protocol version mismatch. Expected: " + version + ", got: " + v);
            }
        }

        @Override
        public void write(ByteBuffer buffer, byte[] payload) {
            buffer.put(version);
        }

        @Override
        public int length() {
            return 1;
        }
    }
}
