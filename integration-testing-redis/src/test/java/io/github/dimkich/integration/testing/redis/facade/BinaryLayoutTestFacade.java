package io.github.dimkich.integration.testing.redis.facade;

import io.github.dimkich.integration.testing.redis.codec.RedisDataCodec;
import io.github.dimkich.integration.testing.redis.codec.segment.BinaryFormatParser;
import io.github.dimkich.integration.testing.redis.codec.segment.BinarySegment;
import io.github.dimkich.integration.testing.redis.codec.segment.EnvelopedBinaryCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HexFormat;
import java.util.List;

@RequiredArgsConstructor
@Component("binaryLayoutTestFacade")
public class BinaryLayoutTestFacade {
    private final BinaryFormatParser parser;
    private final HexFormat hexFormat = HexFormat.of().withUpperCase();

    final RedisDataCodec rawCodec = new RedisDataCodec() {
        @Override
        public byte[] serialize(Object object) {
            return (byte[]) object;
        }

        @Override
        public Object deserialize(byte[] data) {
            return data;
        }
    };

    public String serialize(String layout, String payloadHex) {
        byte[] payload = parseHex(payloadHex);
        List<BinarySegment> segments = parser.parse(layout);
        EnvelopedBinaryCodec envelopedCodec = new EnvelopedBinaryCodec(rawCodec, segments);

        byte[] result = envelopedCodec.serialize(payload);
        return formatHex(result);
    }

    public String deserialize(String layout, String payloadHex) {
        byte[] data = parseHex(payloadHex);
        List<BinarySegment> segments = parser.parse(layout);
        EnvelopedBinaryCodec envelopedCodec = new EnvelopedBinaryCodec(rawCodec, segments);

        byte[] result = (byte[]) envelopedCodec.deserialize(data);
        return formatHex(result);
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
