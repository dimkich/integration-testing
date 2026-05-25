package io.github.dimkich.integration.testing.redis.registry;

import io.github.dimkich.integration.testing.redis.codec.RedisDataCodec;
import io.github.dimkich.integration.testing.redis.codec.segment.BinaryFormatParser;
import io.github.dimkich.integration.testing.redis.codec.segment.EnvelopedBinaryCodec;
import io.github.dimkich.integration.testing.redis.config.RedisProperties;
import io.github.dimkich.integration.testing.redis.schema.ComposedRedisDataSchema;
import io.github.dimkich.integration.testing.redis.schema.RedisDataSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

/**
 * Wraps {@link RedisDataCodec} and {@link RedisDataSchema} instances with binary envelope
 * codecs when a format configuration string is present.
 * <p>
 * Format strings are parsed by {@link BinaryFormatParser} and applied via
 * {@link EnvelopedBinaryCodec} for values and hash fields. When no format is configured,
 * the original codec or schema is returned unchanged.
 *
 * @see EnvelopedBinaryCodec
 * @see ComposedRedisDataSchema
 */
@RequiredArgsConstructor
public class BinaryEnvelopeService {
    private final BinaryFormatParser binaryFormatParser;

    /**
     * Wraps a codec with an enveloped binary layer when {@code formatConfig} is non-empty.
     *
     * @param codec        the base codec (may be {@code null})
     * @param formatConfig binary format descriptor; blank means no wrapping
     * @return enveloped codec, or {@code codec} unchanged when wrapping does not apply
     */
    public RedisDataCodec wrapCodec(RedisDataCodec codec, String formatConfig) {
        if (codec != null && StringUtils.hasText(formatConfig)) {
            return new EnvelopedBinaryCodec(codec, binaryFormatParser.parse(formatConfig));
        }
        return codec;
    }

    /**
     * Wraps value, hash-key, and hash-value codecs of a schema when any corresponding
     * binary format is configured on {@code props}.
     *
     * @param schema the base schema
     * @param props  schema properties carrying optional {@code *BinaryFormat} fields
     * @return a {@link ComposedRedisDataSchema} with enveloped codecs, or {@code schema} unchanged
     */
    public RedisDataSchema wrapSchema(RedisDataSchema schema, RedisProperties.Schema props) {
        if (!StringUtils.hasText(props.getValueBinaryFormat())
                && !StringUtils.hasText(props.getHashKeyBinaryFormat())
                && !StringUtils.hasText(props.getHashValueBinaryFormat())) {
            return schema;
        }

        return new ComposedRedisDataSchema(
                wrapCodec(schema.getValueCodec(), props.getValueBinaryFormat()),
                wrapCodec(schema.getHashKeyCodec(), props.getHashKeyBinaryFormat()),
                wrapCodec(schema.getHashValueCodec(), props.getHashValueBinaryFormat())
        );
    }
}