package io.github.dimkich.integration.testing.redis.serde;

import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Encoder/decoder for a single Redis data component (value, hash key or hash value):
 * a pair of core serde converters.
 * <p>
 * Both directions come from one resolution with the same serde configuration, so the
 * serializer and the deserializer of a component always share the same format.
 * {@link RedisSchemaSerdeFactory} joins the converters resolved through
 * {@link io.github.dimkich.integration.testing.serde.SerdeManager} into a codec, and
 * {@link RedisDataSchema} composes three codecs — one per component.
 */
@Getter
@RequiredArgsConstructor
public class RedisDataCodec {
    /** Serializes a Java object into raw Redis bytes. */
    private final TestSerdeConverter<Object, byte[], TestSerdeContext> serializer;

    /** Deserializes raw Redis bytes into a Java object. */
    private final TestSerdeConverter<byte[], Object, TestSerdeContext> deserializer;

    /**
     * Deserializes raw Redis bytes into a Java object.
     *
     * @param data raw bytes from Redis, may be null or empty
     * @return the deserialized object
     */
    public Object deserialize(byte[] data) {
        return deserializer.convert(data, TestSerdeContext.EMPTY);
    }

    /**
     * Serializes a Java object into raw bytes for storage in Redis.
     *
     * @param object the object to serialize, may be null
     * @return the serialized bytes
     */
    public byte[] serialize(Object object) {
        return serializer.convert(object, TestSerdeContext.EMPTY);
    }
}
