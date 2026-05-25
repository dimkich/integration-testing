package io.github.dimkich.integration.testing.redis.codec.segment;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Provider for the {@code {CONTENT}} format tag in a binary envelope layout.
 * <p>
 * Produces a variable-length segment that reads and writes the payload bytes. When reading,
 * the payload length is taken from {@link ReadContext#getPayloadLength()} if set (typically
 * by a preceding {@link LengthSegmentProvider} segment). If payload length is {@code -1},
 * the remaining bytes minus the suffix size are consumed.
 * <p>
 * This provider accepts no parameters; {@code {CONTENT}} creates a content segment directly.
 *
 * @see BinarySegmentProvider
 * @see LengthSegmentProvider
 * @see ReadContext
 */
public class ContentSegmentProvider implements BinarySegmentProvider {
    @Override
    public String getName() {
        return "CONTENT";
    }

    @Override
    public BinarySegment create(List<String> params) {
        return new ContentSegment();
    }

    /**
     * Variable-length segment that holds the envelope payload bytes.
     * Reads from buffer into {@link ReadContext#setPayload(byte[])}; writes the payload to buffer.
     */
    static class ContentSegment implements BinarySegment {
        @Override
        public void read(ByteBuffer buffer, ReadContext ctx) {
            int length = ctx.getPayloadLength();
            if (length == -1) {
                length = buffer.remaining() - ctx.getSuffixSize();
            }

            byte[] payload = new byte[length];
            buffer.get(payload);
            ctx.setPayload(payload);
        }

        @Override
        public void write(ByteBuffer buffer, byte[] payload) {
            buffer.put(payload);
        }

        @Override
        public int length() {
            return 0;
        }
    }
}
