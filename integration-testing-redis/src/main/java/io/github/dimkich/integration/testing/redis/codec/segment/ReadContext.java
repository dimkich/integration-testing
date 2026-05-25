package io.github.dimkich.integration.testing.redis.codec.segment;

import lombok.Data;

/**
 * Mutable context passed to {@link BinarySegment#read} during envelope deserialization.
 * Segments use and update these fields to coordinate reading header, payload, and footer.
 * <p>
 * Typical flow: {@link LengthSegmentProvider} sets {@link #payloadLength}; {@link ContentSegmentProvider}
 * reads it to determine how many bytes to consume and stores the result in {@link #payload}. The
 * caller ({@link EnvelopedBinaryCodec}) sets {@link #suffixSize} to the sum of fixed footer segment
 * lengths so that content can compute "remaining bytes minus suffix" when payload length is absent.
 *
 * @see BinarySegment
 * @see EnvelopedBinaryCodec
 * @see LengthSegmentProvider
 * @see ContentSegmentProvider
 */
@Data
public class ReadContext {
    /**
     * Length of the payload in bytes, or {@code -1} if not yet known (e.g. no length-prefix segment).
     * Set by {@link LengthSegmentProvider}; used by {@link ContentSegmentProvider}.
     */
    private int payloadLength = -1;

    /**
     * Sum of fixed lengths of segments after the content segment (footer size).
     * Used when {@link #payloadLength} is {@code -1} to compute payload as
     * {@code buffer.remaining() - suffixSize}.
     */
    private int suffixSize = 0;

    /**
     * The extracted payload bytes, set by {@link ContentSegmentProvider} after reading the content segment.
     */
    private byte[] payload;
}
