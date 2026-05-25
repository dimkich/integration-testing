package io.github.dimkich.integration.testing.redis.codec.segment;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Provider for the {@code {VER}} format tag in a binary envelope layout.
 * <p>
 * Produces a single-byte version segment. When reading, the segment validates that the byte matches
 * the expected version and throws if it does not. When writing, it emits the configured version byte.
 * <p>
 * Parameters:
 * <ul>
 *   <li>{@code version} (required) — protocol version byte (decimal or hex, e.g. {@code 1} or {@code 0x01})</li>
 * </ul>
 * <p>
 * Example: {@code {VER(1)}} creates a 1-byte segment expecting and emitting version 1.
 *
 * @see BinarySegmentProvider
 * @see BinaryFormatParser
 */
public class VersionSegmentProvider implements BinarySegmentProvider {
    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "VER";
    }

    /**
     * {@inheritDoc}
     * @param params first element is the version byte (required)
     */
    @Override
    public BinarySegment create(List<String> params) {
        byte version = (byte) Integer.decode(params.get(0)).intValue();
        return new VersionSegment(version);
    }

    /** Single-byte version segment that validates on read and emits the version on write. */
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
