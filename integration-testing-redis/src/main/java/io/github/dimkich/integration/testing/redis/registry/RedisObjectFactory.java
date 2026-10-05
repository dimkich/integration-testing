package io.github.dimkich.integration.testing.redis.registry;

import io.github.dimkich.integration.testing.config.PropertyInheritanceMerger;
import io.github.dimkich.integration.testing.redis.config.RedisSchemaProperties;
import io.github.dimkich.integration.testing.redis.serde.RedisSchemaSerdeFactory;
import io.github.dimkich.integration.testing.storage.exclusion.FieldExclusionProcessor;
import lombok.RequiredArgsConstructor;

/**
 * Creates {@link RedisDataSchemaMetadata} from {@link RedisSchemaProperties}: applies terminal
 * defaults, compiles the field-exclusion tree and delegates schema assembly to
 * {@link RedisSchemaSerdeFactory}. Created instances are cached by
 * {@link RedisDataSchemaRegistry}.
 * <p>
 * The configuration must already be merged with its parents (see
 * {@link PropertyInheritanceMerger}); the default source is applied here, after the whole
 * inheritance chain has been merged, so lazily created connections are finalized as well.
 *
 * @see RedisSchemaProperties
 * @see RedisSchemaSerdeFactory
 * @see RedisDataSchemaMetadata
 */
@RequiredArgsConstructor
public class RedisObjectFactory {
    private final FieldExclusionProcessor fieldExclusionProcessor;
    private final RedisSchemaSerdeFactory schemaSerdeFactory;
    private final PropertyInheritanceMerger merger;

    /**
     * Creates data-schema metadata from a schema or per-key-pattern configuration entry.
     *
     * @param props schema properties ({@code null} yields {@code null})
     * @return schema metadata with ignore flag and field exclusions,
     *         or {@code null} if {@code props} is {@code null}
     * @throws IllegalArgumentException if a source cannot be resolved
     */
    public RedisDataSchemaMetadata createSchema(RedisSchemaProperties props) {
        if (props == null) {
            return null;
        }
        props.applyDefaultSource(merger);
        return new RedisDataSchemaMetadata(
                schemaSerdeFactory.createSchema(props),
                props.isIgnore(),
                fieldExclusionProcessor.compile(props.getExcludedFields()));
    }
}
