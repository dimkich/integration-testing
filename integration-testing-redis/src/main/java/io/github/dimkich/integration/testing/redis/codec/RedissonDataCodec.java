package io.github.dimkich.integration.testing.redis.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.redisson.client.protocol.Decoder;
import org.redisson.client.protocol.Encoder;

/**
 * {@link RedisDataCodec} implementation that delegates serialization and deserialization
 * to Redisson's {@link Encoder} and {@link Decoder}.
 * <p>
 * Wraps raw byte arrays in Netty {@link ByteBuf} instances when invoking the underlying
 * Redisson codec, enabling compatibility with any Redisson codec (e.g. FstCodec, JsonJacksonCodec).
 * <p>
 * Typically created by {@link RedissonCodecAdapter} from a Redisson {@code Codec} bean.
 */
@RequiredArgsConstructor
public class RedissonDataCodec implements RedisDataCodec {
    private final Encoder encoder;
    private final Decoder<Object> decoder;

    /**
     * {@inheritDoc}
     * <p>
     * Delegates to the configured Redisson decoder, wrapping the input bytes in a
     * Netty {@link ByteBuf} for decoding.
     */
    @Override
    @SneakyThrows
    public Object deserialize(byte[] data) {
        return data == null ? null : decoder.decode(Unpooled.wrappedBuffer(data), null);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Delegates to the configured Redisson encoder and extracts bytes from the
     * resulting Netty {@link ByteBuf}. The buffer is released after use.
     */
    @Override
    @SneakyThrows
    public byte[] serialize(Object object) {
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
