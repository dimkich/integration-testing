package io.github.dimkich.integration.testing.redis.registry;

import io.github.dimkich.integration.testing.redis.schema.RedisDataSchema;
import io.github.dimkich.integration.testing.storage.exclusion.FieldExclusionTree;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Metadata for a Redis key schema, including serialization rules and field exclusions.
 * <p>
 * Instances are registered in {@link ConnectionSchemaRegistry} and selected via longest-prefix
 * matching on Redis key patterns. Each metadata holds:
 * <ul>
 *   <li>a {@link RedisDataSchema} that defines codecs for value, hash key, and hash value serialization</li>
 *   <li>an ignore flag indicating whether matching keys should be excluded from assertions</li>
 *   <li>a {@link FieldExclusionTree} for excluding specific fields when processing stored data</li>
 * </ul>
 *
 * @see ConnectionSchemaRegistry
 * @see RedisDataSchema
 * @see FieldExclusionTree
 */
@Getter
@RequiredArgsConstructor
public class RedisDataSchemaMetadata {
    /** {@link RedisDataSchema} resolved from configuration; defines value and hash codecs. */
    private final RedisDataSchema schema;

    /** When {@code true}, keys matching this metadata are excluded from assertions and processing. */
    private final boolean ignore;

    /** Tree of dot-separated field paths to exclude when processing or comparing stored data. */
    private final FieldExclusionTree fieldExclusion;
}
