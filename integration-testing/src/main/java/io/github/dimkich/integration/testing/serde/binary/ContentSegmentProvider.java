package io.github.dimkich.integration.testing.serde.binary;

import lombok.Getter;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Provider for the {@code {CONTENT}} tag: a variable-length segment holding the payload. The
 * length comes from {@link ReadContext#getPayloadLength()} when set, otherwise the remaining
 * bytes minus the suffix size are consumed. A negative length or a length exceeding the
 * remaining bytes is rejected as a corrupted frame. Accepts no parameters.
 *
 * <p>On read the segment returns a {@link ByteBuffer} view over the input and does not copy the
 * payload; on write it copies the payload bytes into the envelope buffer.
 */
@Getter
public class ContentSegmentProvider implements BinarySegmentProvider {

    private final String name = "CONTENT";

    @Override
    public BinarySegment create(List<String> params) {
        return new ContentSegment();
    }

    static class ContentSegment implements BinarySegment {
        @Override
        public void read(ByteBuffer buffer, ReadContext ctx) {
            int length = ctx.getPayloadLength();
            if (length == -1) {
                length = buffer.remaining() - ctx.getSuffixSize();
            }
            if (length < 0 || length > buffer.remaining()) {
                throw new RuntimeException("Corrupted frame: invalid payload length " + length);
            }

            ByteBuffer payload = buffer.slice();
            payload.limit(length);
            buffer.position(buffer.position() + length);
            ctx.setPayload(payload);
        }

        @Override
        public void write(ByteBuffer buffer, ByteBuffer payload) {
            buffer.put(payload);
        }

        @Override
        public int length() {
            return 0;
        }
    }
}
