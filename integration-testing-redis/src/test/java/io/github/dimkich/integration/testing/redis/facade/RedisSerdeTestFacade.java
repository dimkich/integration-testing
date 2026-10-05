package io.github.dimkich.integration.testing.redis.facade;

import io.github.dimkich.integration.testing.format.common.type.TypeParser;
import io.github.dimkich.integration.testing.redis.registry.RedisDataSchemaMetadata;
import io.github.dimkich.integration.testing.redis.registry.RedisDataSchemaRegistry;
import io.github.dimkich.integration.testing.redis.serde.RedisDataCodec;
import io.github.dimkich.integration.testing.redis.serde.RedisDataSchema;
import io.github.dimkich.integration.testing.serde.SerdeManager;
import io.github.dimkich.integration.testing.serde.StandardSerdeProperties;
import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.HexFormat;

/**
 * Facade for the {@code Serde} test container: exercises Redis serde resolution directly,
 * without going through Redis storage.
 *
 * <p>Two levels are covered:
 * <ul>
 *   <li><b>Component</b> — a {@link StandardSerdeProperties} with a single-slot source is
 *       resolved to a core serializer/deserializer pair, optionally with a target class and
 *       an object-mapper reference.</li>
 *   <li><b>Schema</b> — a {@link RedisDataSchema} resolved by the registry is used per
 *       component (value, hash key, hash value).</li>
 * </ul>
 *
 * <p>Empty request arguments mean "not set", so a single method covers provider names,
 * fully qualified class names and bean references.
 */
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class RedisSerdeTestFacade {
    private static final String NULL = "<null>";
    private static final HexFormat HEX = HexFormat.of().withUpperCase();

    private final SerdeManager serdeManager;
    private final RedisDataSchemaRegistry schemaRegistry;
    private final TypeParser typeParser;

    /** Component-level round-trip: serialize and deserialize {@code data}. */
    public String roundTripComponent(String beanRef, String type, String binaryEnvelope, String data) {
        StandardSerdeProperties props = component(beanRef, type, binaryEnvelope);
        TestSerdeConverter<Object, byte[], TestSerdeContext> serializer = componentSerializer(props);
        TestSerdeConverter<byte[], Object, TestSerdeContext> deserializer = componentDeserializer(props);
        return text(deserializer.convert(serializer.convert(data, TestSerdeContext.EMPTY), TestSerdeContext.EMPTY));
    }

    /** Component-level round-trip of a typed value through a provider. */
    public Object roundTripComponentTyped(String type, String targetClass, String objectMapperRef, Object data) {
        StandardSerdeProperties props = componentTyped(type, targetClass, objectMapperRef);
        TestSerdeConverter<Object, byte[], TestSerdeContext> serializer = componentSerializer(props);
        TestSerdeConverter<byte[], Object, TestSerdeContext> deserializer = componentDeserializer(props);
        return deserializer.convert(serializer.convert(data, TestSerdeContext.EMPTY), TestSerdeContext.EMPTY);
    }

    /** Component-level serialization; returns the bytes as uppercase hex. */
    public String serializeComponentHex(String beanRef, String type, String binaryEnvelope, String data) {
        TestSerdeConverter<Object, byte[], TestSerdeContext> serializer =
                componentSerializer(component(beanRef, type, binaryEnvelope));
        return hex(serializer.convert(data, TestSerdeContext.EMPTY));
    }

    /** Schema-level serialization of the requested component; returns the bytes as uppercase hex. */
    public String serializeSchemaForKeyHex(String connection, String key, String component, String data) {
        RedisDataCodec codec = componentCodec(schemaRegistry.findSchema(connection, key).getSchema(), component);
        return hex(codec.serialize(data));
    }

    /** Schema-level round-trip for the schema selected for a connection and key. */
    public String roundTripSchemaForKey(String connection, String key, String component, String data) {
        RedisDataCodec codec = componentCodec(schemaRegistry.findSchema(connection, key).getSchema(), component);
        return text(codec.deserialize(codec.serialize(data)));
    }

    /** Describes the codecs of the schema selected for a connection and key. */
    public String describeSchemaForKey(String connection, String key) {
        RedisDataSchemaMetadata metadata = schemaRegistry.findSchema(connection, key);
        RedisDataSchema schema = metadata.getSchema();
        return "ignore=" + metadata.isIgnore()
                + ", value=" + codecName(schema.getValueCodec())
                + ", hashKey=" + codecName(schema.getHashKeyCodec())
                + ", hashValue=" + codecName(schema.getHashValueCodec());
    }

    /** Resolves a component-level config and returns the diagnostic if it fails. */
    public String errorOfComponent(String beanRef, String type, String binaryEnvelope) {
        try {
            TestSerdeConverter<?, ?, ?> serializer =
                    componentSerializer(component(beanRef, type, binaryEnvelope));
            return "no error: " + (serializer == null ? NULL : serializer.getClass().getSimpleName());
        } catch (RuntimeException e) {
            return describe(e);
        }
    }

    /** Resolves a typed component-level config and returns the diagnostic if it fails. */
    public String errorOfComponentTyped(String type, String targetClass, String objectMapperRef) {
        try {
            TestSerdeConverter<?, ?, ?> serializer =
                    componentSerializer(componentTyped(type, targetClass, objectMapperRef));
            return "no error: " + (serializer == null ? NULL : serializer.getClass().getSimpleName());
        } catch (RuntimeException e) {
            return describe(e);
        }
    }

    private TestSerdeConverter<Object, byte[], TestSerdeContext> componentSerializer(StandardSerdeProperties props) {
        return serdeManager.resolve(props, Object.class, byte[].class, TestSerdeContext.class, null);
    }

    private TestSerdeConverter<byte[], Object, TestSerdeContext> componentDeserializer(StandardSerdeProperties props) {
        return serdeManager.resolve(props, byte[].class, Object.class, TestSerdeContext.class, null);
    }

    private RedisDataCodec componentCodec(RedisDataSchema schema, String component) {
        return switch (component) {
            case "value" -> schema.getValueCodec();
            case "hash-key" -> schema.getHashKeyCodec();
            case "hash-value" -> schema.getHashValueCodec();
            default -> throw new IllegalArgumentException("Unknown Redis schema component: " + component);
        };
    }

    private StandardSerdeProperties component(String beanRef, String type, String binaryEnvelope) {
        StandardSerdeProperties props = new StandardSerdeProperties();
        if (StringUtils.hasText(beanRef)) {
            props.setBeanRef(beanRef);
        }
        if (StringUtils.hasText(type)) {
            props.setType(type);
        }
        if (StringUtils.hasText(binaryEnvelope)) {
            props.setBinaryEnvelope(binaryEnvelope);
        }
        return props;
    }

    private StandardSerdeProperties componentTyped(String type, String targetClass, String objectMapperRef) {
        StandardSerdeProperties props = component(null, type, null);
        if (StringUtils.hasText(targetClass)) {
            props.setTargetClass(typeParser.parse(targetClass));
        }
        if (StringUtils.hasText(objectMapperRef)) {
            props.setObjectMapperRef(objectMapperRef);
        }
        return props;
    }

    private String codecName(RedisDataCodec codec) {
        return codec == null ? NULL : codec.getClass().getSimpleName();
    }

    private String text(Object value) {
        return value == null ? NULL : String.valueOf(value);
    }

    private String hex(byte[] data) {
        return data == null ? NULL : HEX.formatHex(data);
    }

    private String describe(RuntimeException e) {
        String message = e.getMessage() == null ? NULL
                : e.getMessage().replace("\r\n", " | ").replace("\n", " | ");
        return e.getClass().getSimpleName() + ": " + message;
    }
}
