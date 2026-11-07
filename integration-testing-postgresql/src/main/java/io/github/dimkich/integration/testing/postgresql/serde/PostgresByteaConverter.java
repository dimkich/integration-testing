package io.github.dimkich.integration.testing.postgresql.serde;

import io.github.dimkich.integration.testing.postgresql.model.metadata.PostgresColumnSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * Converts PostgreSQL {@code bytea} values to their binary representation.
 */
public final class PostgresByteaConverter extends PostgresConverter {

    private static final HexFormat HEX = HexFormat.of();
    private static final String HEX_PREFIX = "\\x";

    @Override
    public byte[] convert(ByteBuffer input, PostgresColumnSerdeContext context) {
        byte[] bytes = TestSerdeConverter.toByteArray(input);
        if (bytes == null || !hasHexPrefix(bytes)) {
            return bytes;
        }
        try {
            return HEX.parseHex(new String(bytes, HEX_PREFIX.length(),
                    bytes.length - HEX_PREFIX.length(), StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            return bytes;
        }
    }

    private static boolean hasHexPrefix(byte[] bytes) {
        return bytes.length >= HEX_PREFIX.length()
                && bytes[0] == '\\'
                && bytes[1] == 'x';
    }
}
