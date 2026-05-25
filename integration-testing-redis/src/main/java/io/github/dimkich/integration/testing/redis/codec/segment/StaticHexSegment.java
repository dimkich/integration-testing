package io.github.dimkich.integration.testing.redis.codec.segment;

import lombok.RequiredArgsConstructor;

import java.nio.ByteBuffer;
import java.util.HexFormat;

/**
 * A fixed-length binary segment whose byte sequence is defined by a hex string literal.
 * <p>
 * Used to represent constant markers in envelope layouts (e.g. magic bytes, version stamps).
 * The segment always reads and writes the same byte sequence; it does not depend on payload.
 *
 * @see BinarySegment
 * @see BinaryFormatParser
 */
@RequiredArgsConstructor
public class StaticHexSegment implements BinarySegment {
    private static final HexFormat hexFormat = HexFormat.of();
    private final byte[] staticBytes;

    /**
     * Creates a segment from a hex string (e.g. {@code "5244"} for Redis RDB magic).
     *
     * @param hex hex-encoded bytes (even number of hex digits, no separator)
     */
    public StaticHexSegment(String hex) {
        this.staticBytes = hexFormat.parseHex(hex);
    }

    /** {@inheritDoc} Advances the buffer position past the static bytes without storing them. */
    @Override
    public void read(ByteBuffer buffer, ReadContext ctx) {
        buffer.position(buffer.position() + staticBytes.length);
    }

    /** {@inheritDoc} Writes the fixed static bytes; payload is ignored. */
    @Override
    public void write(ByteBuffer buffer, byte[] payload) {
        buffer.put(staticBytes);
    }

    /** {@inheritDoc} Returns the fixed length of the static byte sequence. */
    @Override
    public int length() {
        return staticBytes.length;
    }
}
