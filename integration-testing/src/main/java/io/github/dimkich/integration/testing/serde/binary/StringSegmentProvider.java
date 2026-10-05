package io.github.dimkich.integration.testing.serde.binary;

import lombok.Getter;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

/**
 * Provider for the {@code {STR}} tag: a fixed ({@code {STR(text)}}) or length-prefixed
 * ({@code {STR(text, type, order)}}) UTF-8 string literal.
 */
@Getter
public class StringSegmentProvider implements BinarySegmentProvider {

    private final String name = "STR";

    @Override
    public BinarySegment create(List<String> params) {
        byte[] stringBytes = params.get(0).getBytes(StandardCharsets.UTF_8);
        if (params.size() > 1) {
            BinaryDataType type = BinaryDataType.fromString(params.get(1));
            BinaryByteOrder order = BinaryByteOrder.fromString(params.get(2));
            return new DynamicStringSegment(stringBytes, type, order);
        }
        return new StaticHexSegment(stringBytes);
    }

    private record DynamicStringSegment(byte[] bytes, BinaryDataType type,
                                        BinaryByteOrder order) implements BinarySegment {
        private static final HexFormat hexFormat = HexFormat.of().withUpperCase();

        @Override
        public void read(ByteBuffer buffer, ReadContext ctx) {
            order.apply(buffer);
            int stringLen = type.read(buffer).intValue();
            byte[] actual = new byte[stringLen];
            buffer.get(actual);
            if (!Arrays.equals(actual, bytes)) {
                throw new RuntimeException("String mismatch. Expected: " + hexFormat.formatHex(bytes)
                        + ", got: " + hexFormat.formatHex(actual));
            }
        }

        @Override
        public void write(ByteBuffer buffer, byte[] payload) {
            order.apply(buffer);
            type.write(buffer, bytes.length);
            buffer.put(bytes);
        }

        @Override
        public int length() {
            return type.getSize() + bytes.length;
        }
    }
}
