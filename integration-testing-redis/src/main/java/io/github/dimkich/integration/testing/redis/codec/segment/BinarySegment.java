package io.github.dimkich.integration.testing.redis.codec.segment;

import java.nio.ByteBuffer;

/**
 * A logical part of a binary envelope format used to encode and decode Redis data with custom layouts.
 * <p>
 * Segments are composed in sequence (e.g. version + length + content + checksum) and parsed from
 * a format string by {@link BinaryFormatParser}. Each segment implements {@link #read}, {@link #write},
 * and {@link #length} to participate in deserialization, serialization, and header/footer size calculation.
 * <p>
 * Typical implementations include {@code {LEN}} for length prefixes, {@code {CONTENT}} for payload,
 * {@code {VER}}, {@code {TS}}, {@code {CRC32}}, {@code {FIX}}, and static hex literals.
 *
 * @see BinaryFormatParser
 * @see BinarySegmentProvider
 * @see EnvelopedBinaryCodec
 */
public interface BinarySegment {
    /**
     * Reads this segment from the buffer and updates the read context.
     * May consume bytes (e.g. length field, version) or populate context fields (e.g. payload length
     * for a length prefix, payload bytes for a content segment).
     *
     * @param buffer the source buffer (position advances as bytes are consumed)
     * @param ctx    mutable context shared across segments; may carry payload length, suffix size, payload
     */
    void read(ByteBuffer buffer, ReadContext ctx);

    /**
     * Writes this segment to the buffer.
     * For fixed/header segments (version, length, checksum) writes their bytes; for content segments
     * writes the payload.
     *
     * @param buffer  the target buffer (position advances as bytes are written)
     * @param payload the serialized payload; used by content segment and length-prefix segments
     */
    void write(ByteBuffer buffer, byte[] payload);

    /**
     * Returns the fixed byte size of this segment when encoding.
     * Used to compute header and footer sizes for envelope layout. Variable-length segments
     * (e.g. content) return {@code 0}.
     *
     * @return the number of fixed bytes this segment occupies, or 0 if variable-length
     */
    int length();
}
