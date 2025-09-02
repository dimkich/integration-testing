package io.github.dimkich.integration.testing.format.common.mixin;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.util.StdConverter;
import lombok.SneakyThrows;

import java.io.ByteArrayInputStream;

@JsonSerialize(converter = ByteArrayInputStreamMixIn.FromStream.class)
@JsonDeserialize(converter = ByteArrayInputStreamMixIn.ToStream.class)
public class ByteArrayInputStreamMixIn {
    public static class FromStream extends StdConverter<ByteArrayInputStream, byte[]> {
        @Override
        @SneakyThrows
        public byte[] convert(ByteArrayInputStream value) {
            value.reset();
            return value.readAllBytes();
        }
    }

    public static class ToStream extends StdConverter<byte[], ByteArrayInputStream> {
        @Override
        public ByteArrayInputStream convert(byte[] value) {
            return new ByteArrayInputStream(value);
        }
    }
}
