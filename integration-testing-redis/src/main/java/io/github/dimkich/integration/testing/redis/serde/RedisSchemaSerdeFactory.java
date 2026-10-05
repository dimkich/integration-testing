package io.github.dimkich.integration.testing.redis.serde;

import io.github.dimkich.integration.testing.redis.config.RedisSchemaProperties;
import io.github.dimkich.integration.testing.serde.SerdeManager;
import io.github.dimkich.integration.testing.serde.StandardSerdeProperties;
import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import lombok.RequiredArgsConstructor;

/**
 * Single assembly point for Redis key codecs and data schemas: resolves each configured
 * component of a {@link RedisSchemaProperties} through {@link SerdeManager} and joins the
 * resulting serializer/deserializer pairs into {@link RedisDataCodec codecs} of a
 * {@link RedisDataSchema}.
 * <p>
 * Component inheritance is handled before this factory runs: the global default schema, the
 * connection default schema and each per-key-pattern schema are merged by the general
 * property-merge rules (see
 * {@link io.github.dimkich.integration.testing.config.PropertyInheritanceMerger}), so every
 * component reaching the factory is already fully configured. The resolved schema is cached by
 * {@link io.github.dimkich.integration.testing.redis.registry.RedisDataSchemaRegistry}.
 *
 * @see RedisDataCodec
 * @see RedisDataSchema
 * @see RedisSchemaProperties
 */
@RequiredArgsConstructor
public class RedisSchemaSerdeFactory {
    private final SerdeManager serdeManager;

    /**
     * Resolves the key codec from connection-level configuration.
     *
     * @param props key codec properties ({@code null} yields {@code null})
     * @return the key codec, or {@code null} if {@code props} is {@code null}
     * @throws IllegalArgumentException if no source is configured or the source cannot be resolved
     */
    public RedisDataCodec createKeyCodec(StandardSerdeProperties props) {
        return props == null ? null : codec(props, RedisComponentRole.VALUE);
    }

    /**
     * Assembles a schema from the already merged schema configuration.
     *
     * @param props schema properties ({@code null} yields {@code null})
     * @return the resolved schema, or {@code null} if {@code props} is {@code null}
     * @throws IllegalArgumentException if a source cannot be resolved
     */
    public RedisDataSchema createSchema(RedisSchemaProperties props) {
        if (props == null) {
            return null;
        }
        return new RedisDataSchema(
                codec(props.getValue(), RedisComponentRole.VALUE),
                codec(props.getHashKey(), RedisComponentRole.HASH_KEY),
                codec(props.getHashValue(), RedisComponentRole.HASH_VALUE));
    }

    private RedisDataCodec codec(StandardSerdeProperties props, RedisComponentRole role) {
        return new RedisDataCodec(
                resolve(props, role, Object.class, byte[].class),
                resolve(props, role, byte[].class, Object.class));
    }

    private <I, O> TestSerdeConverter<I, O, TestSerdeContext> resolve(
            StandardSerdeProperties props, RedisComponentRole role, Class<I> inputClass, Class<O> outputClass) {
        return serdeManager.resolve(props, inputClass, outputClass, TestSerdeContext.class, role);
    }
}
