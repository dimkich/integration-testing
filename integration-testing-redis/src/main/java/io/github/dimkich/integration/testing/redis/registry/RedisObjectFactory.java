package io.github.dimkich.integration.testing.redis.registry;

import io.github.dimkich.integration.testing.redis.codec.RedisDataCodec;
import io.github.dimkich.integration.testing.redis.codec.RedisDataCodecAdapter;
import io.github.dimkich.integration.testing.redis.config.RedisProperties;
import io.github.dimkich.integration.testing.redis.schema.RedisDataSchema;
import io.github.dimkich.integration.testing.redis.schema.RedisDataSchemaAdapter;
import io.github.dimkich.integration.testing.storage.exclusion.FieldExclusionProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Factory that builds {@link RedisKeyCodecMetadata} and {@link RedisDataSchemaMetadata} from
 * {@link RedisProperties} configuration.
 * <p>
 * Resolves codec and schema beans via {@link RedisAdapterResolver}, applies optional binary
 * envelope wrapping through {@link BinaryEnvelopeService}, and compiles field-exclusion trees.
 * Created instances are cached by {@link RedisDataSchemaRegistry}.
 *
 * @see RedisProperties.Codec
 * @see RedisProperties.Schema
 */
@Component
@RequiredArgsConstructor
public class RedisObjectFactory {
    private final RedisAdapterResolver redisAdapterResolver;
    private final BinaryEnvelopeService envelopeService;
    private final FieldExclusionProcessor fieldExclusionProcessor;
    private final List<RedisDataCodecAdapter> codecAdapters;
    private final List<RedisDataSchemaAdapter> schemaAdapters;

    /**
     * Creates key-codec metadata from connection-level codec configuration.
     *
     * @param props codec properties ({@code null} yields {@code null})
     * @return wrapped codec metadata with field exclusions, or {@code null} if {@code props} is {@code null}
     */
    public RedisKeyCodecMetadata createCodec(RedisProperties.Codec props) {
        if (props == null) {
            return null;
        }

        RedisDataCodec codec = redisAdapterResolver.lookup(RedisDataCodec.class, codecAdapters)
                .from(props.getBeanRef(), props.getClassRef())
                .with(RedisDataCodecAdapter::tryCreateCodec)
                .adapt();

        return new RedisKeyCodecMetadata(
                envelopeService.wrapCodec(codec, props.getValueBinaryFormat()),
                fieldExclusionProcessor.compile(props.getExcludedFields())
        );
    }

    /**
     * Creates data-schema metadata from a schema or per-key-pattern configuration entry.
     *
     * @param props schema properties ({@code null} yields {@code null})
     * @return wrapped schema metadata with ignore flag and field exclusions,
     *         or {@code null} if {@code props} is {@code null}
     */
    public RedisDataSchemaMetadata createSchema(RedisProperties.Schema props) {
        if (props == null) {
            return null;
        }

        RedisDataSchema schema = redisAdapterResolver.lookup(RedisDataSchema.class, schemaAdapters)
                .from(props.getBeanRef(), props.getClassRef())
                .with(RedisDataSchemaAdapter::tryCreateSchema)
                .adapt();

        return new RedisDataSchemaMetadata(
                envelopeService.wrapSchema(schema, props),
                props.isIgnore(),
                fieldExclusionProcessor.compile(props.getExcludedFields())
        );
    }
}