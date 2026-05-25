package io.github.dimkich.integration.testing.redis.codec.segment;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Provider for the {@code {FIX}} format tag in a binary envelope layout.
 * <p>
 * Produces a fixed-size segment of filler bytes. When reading, the segment skips the specified number
 * of bytes. When writing, it emits that many copies of the given byte value (default {@code 0}).
 * <p>
 * Parameters:
 * <ul>
 *   <li>{@code size} (required) — number of bytes in the segment</li>
 *   <li>{@code value} (optional) — byte value to write (decimal or hex, e.g. {@code 0} or {@code 0x00});
 *       defaults to {@code 0} if omitted</li>
 * </ul>
 * <p>
 * Example: {@code {FIX(8, 0)}} creates an 8-byte segment of zeros.
 *
 * @see BinarySegmentProvider
 * @see BinaryFormatParser
 */
public class FixSegmentProvider implements BinarySegmentProvider {
    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "FIX";
    }

    /**
     * {@inheritDoc}
     * @param params first element is size (required), second is byte value (optional, default 0)
     */
    @Override
    public BinarySegment create(List<String> params) {
        int size = Integer.parseInt(params.get(0));
        byte value = (byte) (params.size() > 1 ? Integer.decode(params.get(1)) : 0);
        return new FixSegment(size, value);
    }

    /** Fixed-size filler segment that skips on read and repeats a byte value on write. */
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
