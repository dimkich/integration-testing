package io.github.dimkich.integration.testing.redis.serde.adapter;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.redisson.client.protocol.Decoder;
import org.redisson.client.protocol.Encoder;

/**
 * Internal helper of {@link AbstractRedissonAdapter}: adapts the Redisson {@link Encoder} and
 * {@link Decoder} of a single schema slot to plain byte arrays, wrapping them in Netty
 * {@link ByteBuf} instances when invoking the underlying codec.
 */
@RequiredArgsConstructor
class RedissonSlotCodec {
    private final Encoder encoder;
    private final Decoder<Object> decoder;

    /**
     * Delegates to the Redisson decoder, wrapping the input bytes in a Netty {@link ByteBuf}
     * for decoding.
     */
    @SneakyThrows
    Object deserialize(byte[] data) {
        return data == null ? null : decoder.decode(Unpooled.wrappedBuffer(data), null);
    }

    /**
     * Delegates to the Redisson encoder and extracts bytes from the resulting Netty
     * {@link ByteBuf}. The buffer is released after use.
     */
    @SneakyThrows
    byte[] serialize(Object object) {
        if (object == null) {
            return null;
        }

        ByteBuf buf = encoder.encode(object);
        try {
            return ByteBufUtil.getBytes(buf);
        } finally {
            if (buf != null) {
                buf.release();
            }
        }
    }
}
