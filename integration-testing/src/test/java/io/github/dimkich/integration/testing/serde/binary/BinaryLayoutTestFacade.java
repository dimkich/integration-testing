package io.github.dimkich.integration.testing.serde.binary;

import lombok.RequiredArgsConstructor;

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

    private byte[] parseHex(String hex) {
        if (hex == null) {
            return null;
        }
        return HexFormat.of().parseHex(hex);
    }

    private String formatHex(byte[] data) {
        if (data == null) {
            return null;
        }
        return hexFormat.formatHex(data);
    }
}
