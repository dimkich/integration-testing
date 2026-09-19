package io.github.dimkich.integration.testing.serde;

import io.github.dimkich.integration.testing.format.common.type.TypeParser;
import io.github.dimkich.integration.testing.serde.platform.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Component("serdeTestFacade")
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class SerdeTestFacade {
    public static final String CHANNEL = "test-channel";
    private static final String NULL = "<null>";
    private static final HexFormat HEX = HexFormat.of().withUpperCase();

    private final SerdeManager serdeManager;
    private final TypeParser typeParser;

    public Object roundTrip(String type, String targetClass, String objectMapperRef, Object data) {
        Object serializer = resolveSerializer(config("base", type, null, targetClass, objectMapperRef), "core");
        Object deserializer = resolveDeserializer(config("base", type, null, targetClass, objectMapperRef), "core");
        return deserialize(deserializer, serialize(serializer, data));
    }

    public String serializeText(String type, String targetClass, String objectMapperRef, Object data) {
        Object serializer = resolveSerializer(config("base", type, null, targetClass, objectMapperRef), "core");
        return bytesToText(serialize(serializer, data));
    }

    public Object deserializeText(String type, String targetClass, String objectMapperRef, String text) {
        Object deserializer = resolveDeserializer(config("base", type, null, targetClass, objectMapperRef), "core");
        return deserialize(deserializer, text == null ? null : text.getBytes(StandardCharsets.UTF_8));
    }

    public String serializeBytesHex(String type, String hex) {
        Object serializer = resolveSerializer(config("base", type, null, null, null), "core");
        return bytesToHex(serialize(serializer, hex == null ? null : HEX.parseHex(hex)));
    }

    public String roundTripToString(String type, String targetClass, Object data) {
        Object result = roundTrip(type, targetClass, null, data);
        return result == null ? NULL : String.valueOf(result);
    }

    public String roundTripBytesHex(String type, String hex) {
        Object serializer = resolveSerializer(config("base", type, null, null, null), "core");
        Object deserializer = resolveDeserializer(config("base", type, null, null, null), "core");
        Object result = deserialize(deserializer, serialize(serializer, hex == null ? null : HEX.parseHex(hex)));
        if (result == null) {
            return NULL;
        }
        return result instanceof byte[] bytes ? bytesToHex(bytes) : String.valueOf(result);
    }

    public String serializePlatform(String configKind, String type, String objectMapperRef, Object data) {
        Object serializer = resolveSerializer(config(configKind, type, null, null, objectMapperRef), "platform");
        return bytesToText(((PlatformSerializer) serializer).serialize(CHANNEL, data));
    }

    public String serializePlatformByRef(String beanRef, Object data) {
        Object serializer = resolveSerializer(config("base", null, beanRef, null, null), "platform");
        return bytesToText(((PlatformSerializer) serializer).serialize(CHANNEL, data));
    }

    public String serializePlatformByFqcn(String fqcn, Object data) {
        Object serializer = resolveSerializer(config("base", fqcn, null, null, null), "platform");
        return bytesToText(((PlatformSerializer) serializer).serialize(CHANNEL, data));
    }

    public Object deserializePlatformText(String configKind, String type, String targetClass, String objectMapperRef,
                                          String text) {
        Object deserializer = resolveDeserializer(
                config(configKind, type, null, targetClass, objectMapperRef), "platform");
        return ((PlatformDeserializer) deserializer)
                .deserialize(CHANNEL, text == null ? null : text.getBytes(StandardCharsets.UTF_8));
    }

    public String serializeRecord(String type, String keyPrefix, String headerName, String key, Object value) {
        TestRecordProperties props = new TestRecordProperties();
        props.setType(type);
        props.setKeyPrefix(keyPrefix);
        props.setHeaderName(headerName);
        PlatformRecordSerializer serializer =
                serdeManager.resolveAndAdaptSerializer(props, PlatformRecordSerializer.class, () -> null);
        SerializedRecord record = serializer.serialize(CHANNEL, key, value);
        return record.getChannel() + "|" + record.getKey() + "|" + record.getHeader() + "|"
                + bytesToText(record.getPayload());
    }

    public Object deserializeRecordText(String type, String text) {
        TestRecordProperties props = new TestRecordProperties();
        props.setType(type);
        PlatformRecordDeserializer deserializer =
                serdeManager.resolveAndAdaptDeserializer(props, PlatformRecordDeserializer.class, () -> null);
        return deserializer.deserialize(CHANNEL, "k1", text == null ? null : text.getBytes(StandardCharsets.UTF_8));
    }

    public String resolveClass(String configKind, String type, String objectMapperRef, String targetKind) {
        Object resolved = resolveSerializer(config(configKind, type, null, null, objectMapperRef), targetKind);
        return resolved == null ? NULL : resolved.getClass().getSimpleName();
    }

    public String resolveDefault(String configKind, String type) {
        PlatformSerializer fallback = new DirectPlatformSerializer("DEF:");
        PlatformSerializer serializer = serdeManager.resolveAndAdaptSerializer(
                config(configKind, type, null, null, null), PlatformSerializer.class, () -> fallback);
        return bytesToText(serializer.serialize(CHANNEL, "data"));
    }

    public String errorOf(String configKind, String type, String beanRef, String targetClass,
                          String objectMapperRef, String targetKind) {
        try {
            Object resolved = resolveSerializer(config(configKind, type, beanRef, targetClass, objectMapperRef),
                    targetKind);
            return "no error: " + (resolved == null ? NULL : resolved.getClass().getSimpleName());
        } catch (RuntimeException e) {
            String message = e.getMessage() == null ? NULL
                    : e.getMessage().replace("\r\n", " | ").replace("\n", " | ");
            return e.getClass().getSimpleName() + ": " + message;
        }
    }

    private SerdeProperties config(String configKind, String type, String beanRef, String targetClass,
                                   String objectMapperRef) {
        SerdeProperties props = "record".equals(configKind) ? new TestRecordProperties() : new SerdeProperties();
        if (StringUtils.hasText(type)) {
            props.setType(type);
        }
        if (StringUtils.hasText(beanRef)) {
            props.setBeanRef(beanRef);
        }
        if (StringUtils.hasText(targetClass)) {
            props.setTargetClass(typeParser.parse(targetClass));
        }
        if (StringUtils.hasText(objectMapperRef)) {
            props.setObjectMapperRef(objectMapperRef);
        }
        return props;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private byte[] serialize(Object serializer, Object data) {
        return ((TestSerdeSerializer) serializer).serialize(data, SerdeContext.EMPTY);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object deserialize(Object deserializer, byte[] data) {
        return ((TestSerdeDeserializer) deserializer).deserialize(data, SerdeContext.EMPTY);
    }

    private Object resolveSerializer(SerdeProperties props, String targetKind) {
        return switch (targetKind) {
            case "core" -> serdeManager.resolveAndAdaptSerializer(props, TestSerdeSerializer.class, () -> null);
            case "platform" -> serdeManager.resolveAndAdaptSerializer(props, PlatformSerializer.class, () -> null);
            case "record" -> serdeManager.resolveAndAdaptSerializer(props, PlatformRecordSerializer.class, () -> null);
            case "common" -> serdeManager.resolveAndAdaptSerializer(props, CommonPlatformSerializer.class, () -> null);
            case "alpha" -> serdeManager.resolveAndAdaptSerializer(props, AlphaPlatformSerializer.class, () -> null);
            case "beta" -> serdeManager.resolveAndAdaptSerializer(props, BetaPlatformSerializer.class, () -> null);
            case "runnable" -> serdeManager.resolveAndAdaptSerializer(props, Runnable.class, () -> null);
            default -> throw new IllegalArgumentException("Unknown serializer target kind: " + targetKind);
        };
    }

    private Object resolveDeserializer(SerdeProperties props, String targetKind) {
        return switch (targetKind) {
            case "core" -> serdeManager.resolveAndAdaptDeserializer(props, TestSerdeDeserializer.class, () -> null);
            case "platform" -> serdeManager.resolveAndAdaptDeserializer(props, PlatformDeserializer.class, () -> null);
            case "record" ->
                    serdeManager.resolveAndAdaptDeserializer(props, PlatformRecordDeserializer.class, () -> null);
            default -> throw new IllegalArgumentException("Unknown deserializer target kind: " + targetKind);
        };
    }

    private String bytesToText(byte[] data) {
        return data == null ? NULL : new String(data, StandardCharsets.UTF_8);
    }

    private String bytesToHex(byte[] data) {
        return data == null ? NULL : HEX.formatHex(data);
    }
}
