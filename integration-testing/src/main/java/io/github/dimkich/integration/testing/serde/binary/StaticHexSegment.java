package io.github.dimkich.integration.testing.serde.binary;

import lombok.RequiredArgsConstructor;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HexFormat;

/**
 * A fixed-length binary segment whose byte sequence is defined by a hex string literal, used for
 * constant markers in envelope layouts (e.g. magic bytes). The bytes are validated on read.
 */
@RequiredArgsConstructor
public class StaticHexSegment implements BinarySegment {
    private static final HexFormat hexFormat = HexFormat.of().withUpperCase();
    private final byte[] staticBytes;

    /**
     * Creates a segment from a hex string (e.g. {@code "5244"} for a magic byte sequence).
     *
     * @param hex hex-encoded bytes (even number of hex digits, no separator)
     */
    public StaticHexSegment(String hex) {
        this.staticBytes = hexFormat.parseHex(hex);
    }

    @Override
    public void read(ByteBuffer buffer, ReadContext ctx) {
        byte[] actual = new byte[staticBytes.length];
        buffer.get(actual);
        if (!Arrays.equals(actual, staticBytes)) {
            throw new RuntimeException("Static bytes mismatch. Expected: " + hexFormat.formatHex(staticBytes)
                    + ", got: " + hexFormat.formatHex(actual));
        }
    }

    @Override
    public void write(ByteBuffer buffer, byte[] payload) {
        buffer.put(staticBytes);
    }

    @Override
    public int length() {
        return staticBytes.length;
    }
}
