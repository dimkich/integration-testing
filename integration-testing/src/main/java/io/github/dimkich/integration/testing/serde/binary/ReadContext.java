package io.github.dimkich.integration.testing.serde.binary;

import lombok.Data;

/**
 * Mutable context passed to {@link BinarySegment#read} during envelope deserialization: segments
 * use and update these fields to coordinate reading header, payload and footer.
 *
 * @see BinarySegment
 * @see BinaryEnvelope
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
