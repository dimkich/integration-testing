package io.github.dimkich.integration.testing.redis.registry;

import io.github.dimkich.integration.testing.redis.codec.RedisDataCodec;
import io.github.dimkich.integration.testing.redis.config.RedisProperties;
import io.github.dimkich.integration.testing.storage.exclusion.FieldExclusionTree;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Metadata for Redis key serialization and field exclusions, resolved per connection.
 * <p>
 * Instances are created from {@link RedisProperties.Codec} configuration and cached in
 * {@link RedisDataSchemaRegistry}. Each metadata holds:
 * <ul>
 *   <li>a {@link RedisDataCodec} for serializing and deserializing Redis keys</li>
 *   <li>a {@link FieldExclusionTree} for excluding specific fields when processing or comparing stored data</li>
 * </ul>
 *
 * @see RedisDataSchemaRegistry
 * @see RedisDataCodec
 * @see FieldExclusionTree
 */
@Getter
@RequiredArgsConstructor
public class RedisKeyCodecMetadata {
    /** Codec for Redis key serialization and deserialization. */
    private final RedisDataCodec codec;

    /** Tree of dot-separated field paths to exclude when processing or comparing stored data. */
    private final FieldExclusionTree fieldExclusion;
}
