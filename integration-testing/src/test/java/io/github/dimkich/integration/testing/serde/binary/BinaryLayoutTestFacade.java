package io.github.dimkich.integration.testing.serde.binary;

import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import lombok.RequiredArgsConstructor;

import java.nio.ByteBuffer;
import java.util.HexFormat;

@RequiredArgsConstructor
public class BinaryLayoutTestFacade {
    private final BinaryEnvelopeParser parser;
    private final HexFormat hexFormat = HexFormat.of().withUpperCase();

    public String serialize(String layout, String payloadHex) {
        return formatHex(new BinaryEnvelope(parser.parse(layout)).wrap(parseHex(payloadHex)));
    }

    public String deserialize(String layout, String payloadHex) {
        return formatHex(new BinaryEnvelope(parser.parse(layout)).unwrap(parseHex(payloadHex)));
    }

    private ByteBuffer parseHex(String hex) {
        if (hex == null) {
            return null;
        }
        return TestSerdeConverter.wrap(HexFormat.of().parseHex(hex));
    }

    private String formatHex(ByteBuffer data) {
        return data == null ? null : hexFormat.formatHex(TestSerdeConverter.toByteArray(data));
    }
}
