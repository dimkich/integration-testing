package io.github.dimkich.integration.testing.serde;

import io.github.dimkich.integration.testing.format.common.type.TypeParser;
import io.github.dimkich.integration.testing.serde.platform.*;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@RequiredArgsConstructor
@SuppressWarnings("unused")
public class SerdeTestFacade {
    public static final String CHANNEL = "test-channel";
    private static final String NULL = "<null>";
    private static final HexFormat HEX = HexFormat.of().withUpperCase();

    private final SerdeManager serdeManager;
    private final TypeParser typeParser;
    private final BeanResolver beanResolver;
    private final AdapterManager adapterManager;

    public Object roundTrip(String type, String targetClass, String objectMapperRef, Object data) {
        TestSerdeConverter<Object, byte[], TestSerdeContext> serializer =
                resolveSerializer(config("base", type, null, targetClass, objectMapperRef));
        TestSerdeConverter<byte[], Object, TestSerdeContext> deserializer =
                resolveDeserializer(config("base", type, null, targetClass, objectMapperRef));
        return deserializer.convert(serializer.convert(data, TestSerdeContext.EMPTY), TestSerdeContext.EMPTY);
    }

    public String serializeText(String type, String targetClass, String objectMapperRef, Object data) {
        TestSerdeConverter<Object, byte[], TestSerdeContext> serializer =
                resolveSerializer(config("base", type, null, targetClass, objectMapperRef));
        return bytesToText(serializer.convert(data, TestSerdeContext.EMPTY));
    }

    public Object deserializeText(String type, String targetClass, String objectMapperRef, String text) {
        TestSerdeConverter<byte[], Object, TestSerdeContext> deserializer =
                resolveDeserializer(config("base", type, null, targetClass, objectMapperRef));
        return deserializer.convert(text == null ? null : text.getBytes(StandardCharsets.UTF_8),
                TestSerdeContext.EMPTY);
    }

    public String serializeBytesHex(String type, String hex) {
        TestSerdeConverter<Object, byte[], TestSerdeContext> serializer =
                resolveSerializer(config("base", type, null, null, null));
        return bytesToHex(serializer.convert(hex == null ? null : HEX.parseHex(hex), TestSerdeContext.EMPTY));
    }

    public String roundTripToString(String type, String targetClass, Object data) {
        Object result = roundTrip(type, targetClass, null, data);
        return result == null ? NULL : String.valueOf(result);
    }

    public String roundTripBytesHex(String type, String hex) {
        TestSerdeConverter<Object, byte[], TestSerdeContext> serializer =
                resolveSerializer(config("base", type, null, null, null));
        TestSerdeConverter<byte[], Object, TestSerdeContext> deserializer =
                resolveDeserializer(config("base", type, null, null, null));
        Object result = deserializer.convert(
                serializer.convert(hex == null ? null : HEX.parseHex(hex), TestSerdeContext.EMPTY),
                TestSerdeContext.EMPTY);
        if (result == null) {
            return NULL;
        }
        return result instanceof byte[] bytes ? bytesToHex(bytes) : String.valueOf(result);
    }

    public String serializeByRef(String beanRef, Object data) {
        TestSerdeConverter<Object, byte[], TestSerdeContext> serializer =
                resolveSerializer(config("base", null, beanRef, null, null));
        return bytesToText(serializer.convert(data, TestSerdeContext.EMPTY));
    }

    public String serializePlatform(String configKind, String type, String objectMapperRef, Object data) {
        TestSerdeConverter<Object, byte[], TestPlatformContext> serializer =
                resolvePlatformSerializer(config(configKind, type, null, null, objectMapperRef));
        return bytesToText(serializer.convert(data, new DefaultTestPlatformContext(CHANNEL)));
    }

    public Object deserializePlatformText(String configKind, String type, String targetClass, String objectMapperRef,
                                          String text) {
        TestSerdeConverter<byte[], Object, TestPlatformContext> deserializer =
                resolvePlatformDeserializer(config(configKind, type, null, targetClass, objectMapperRef));
        return deserializer.convert(text == null ? null : text.getBytes(StandardCharsets.UTF_8),
                new DefaultTestPlatformContext(CHANNEL));
    }

    public String deserializePlatformToText(String type, String text) {
        Object result = deserializePlatformText("base", type, null, null, text);
        return result == null ? NULL : String.valueOf(result);
    }

    public String roundTripUnrelated(String text) {
        StandardSerdeProperties props = new StandardSerdeProperties();
        props.setType("unrelated-context");
        TestSerdeConverter<Object, byte[], UnrelatedSerdeContext> serializer =
                serdeManager.resolve(props, Object.class, byte[].class, UnrelatedSerdeContext.class, null);
        TestSerdeConverter<byte[], Object, UnrelatedSerdeContext> deserializer =
                serdeManager.resolve(props, byte[].class, Object.class, UnrelatedSerdeContext.class, null);
        UnrelatedSerdeContext context = new UnrelatedSerdeContext() {
        };
        Object result = deserializer.convert(serializer.convert(text, context), context);
        return result == null ? NULL : String.valueOf(result);
    }

    public String adaptNativeBridge(Object data) {
        TestSerdeConverter<Object, byte[], TestSerdeContext> converter =
                serdeManager.adapt(new NativeBridgeSerializer(), new StandardSerdeProperties(),
                        Object.class, byte[].class, TestSerdeContext.class, null);
        return bytesToText(converter.convert(data, TestSerdeContext.EMPTY));
    }

    public String serializeBinaryHex(String type, String binaryEnvelope, Object data) {
        StandardSerdeProperties props = config("base", type, null, null, null);
        props.setBinaryEnvelope(binaryEnvelope);
        return bytesToHex(resolveSerializer(props).convert(data, TestSerdeContext.EMPTY));
    }

    public Object deserializeBinaryHex(String type, String binaryEnvelope, String targetClass, String hex) {
        StandardSerdeProperties props = config("base", type, null, targetClass, null);
        props.setBinaryEnvelope(binaryEnvelope);
        return resolveDeserializer(props)
                .convert(hex == null ? null : HEX.parseHex(hex), TestSerdeContext.EMPTY);
    }

    public String serializeAdaptedBinaryHex(String beanRef, String binaryEnvelope, Object data) {
        StandardSerdeProperties props = config("base", null, beanRef, null, null);
        props.setBinaryEnvelope(binaryEnvelope);
        return bytesToHex(resolveSerializer(props).convert(data, TestSerdeContext.EMPTY));
    }

    public Object deserializeAdaptedBinaryHex(String beanRef, String binaryEnvelope, String hex) {
        StandardSerdeProperties props = config("base", null, beanRef, null, null);
        props.setBinaryEnvelope(binaryEnvelope);
        return resolveDeserializer(props).convert(hex == null ? null : HEX.parseHex(hex), TestSerdeContext.EMPTY);
    }

    public String errorOfBinary(String configKind, String type, String binaryEnvelope) {
        try {
            StandardSerdeProperties props = config(configKind, type, null, null, null);
            props.setBinaryEnvelope(binaryEnvelope);
            TestSerdeConverter<?, ?, ?> resolved = resolveSerializer(props);
            return "no error: " + (resolved == null ? NULL : resolved.getClass().getSimpleName());
        } catch (RuntimeException e) {
            return formatError(e);
        }
    }

    public String serializeRecord(String type, Object data) {
        return bytesToText(resolveSerializer(record(type)).convert(data, TestSerdeContext.EMPTY));
    }

    public Object deserializeRecord(String type, String text) {
        return resolveDeserializer(record(type))
                .convert(text == null ? null : text.getBytes(StandardCharsets.UTF_8), TestSerdeContext.EMPTY);
    }

    public String resolveClass(String configKind, String type, String objectMapperRef) {
        TestSerdeConverter<?, ?, ?> resolved =
                resolveSerializer(config(configKind, type, null, null, objectMapperRef));
        return resolved == null ? NULL : resolved.getClass().getSimpleName();
    }

    public String resolveDefaultFactoryClass(String format) {
        TestSerdeConverter<?, ?, ?> resolved =
                serdeManager.resolve(new PlainConfig(format), Object.class, byte[].class,
                        TestSerdeContext.class, null);
        return resolved == null ? NULL : resolved.getClass().getSimpleName();
    }

    public String serializeDefaultFactory(String format, String text) {
        TestSerdeConverter<Object, byte[], TestSerdeContext> serializer =
                serdeManager.resolve(new PlainConfig(format), Object.class, byte[].class,
                        TestSerdeContext.class, null);
        return bytesToText(serializer.convert(text, TestSerdeContext.EMPTY));
    }

    public String roundTripDefaultFactory(String format, String text) {
        PlainConfig config = new PlainConfig(format);
        TestSerdeConverter<Object, byte[], TestSerdeContext> serializer =
                serdeManager.resolve(config, Object.class, byte[].class, TestSerdeContext.class, null);
        TestSerdeConverter<byte[], Object, TestSerdeContext> deserializer =
                serdeManager.resolve(config, byte[].class, Object.class, TestSerdeContext.class, null);
        Object result = deserializer.convert(serializer.convert(text, TestSerdeContext.EMPTY),
                TestSerdeContext.EMPTY);
        return result == null ? NULL : String.valueOf(result);
    }

    public String roundTripDefaultFactoryBytes(String hex) {
        PlainConfig config = new PlainConfig("bytes");
        TestSerdeConverter<Object, byte[], TestSerdeContext> serializer =
                serdeManager.resolve(config, Object.class, byte[].class, TestSerdeContext.class, null);
        TestSerdeConverter<byte[], Object, TestSerdeContext> deserializer =
                serdeManager.resolve(config, byte[].class, Object.class, TestSerdeContext.class, null);
        Object result = deserializer.convert(
                serializer.convert(hex == null ? null : HEX.parseHex(hex), TestSerdeContext.EMPTY),
                TestSerdeContext.EMPTY);
        if (result == null) {
            return NULL;
        }
        return result instanceof byte[] bytes ? bytesToHex(bytes) : String.valueOf(result);
    }

    public String errorOfUnconfiguredConfig() {
        try {
            TestSerdeConverter<?, ?, ?> resolved =
                    serdeManager.resolve(new UnconfiguredConfig(), Object.class, byte[].class,
                            TestSerdeContext.class, null);
            return "no error: " + (resolved == null ? NULL : resolved.getClass().getSimpleName());
        } catch (RuntimeException e) {
            return formatError(e);
        }
    }

    public String errorOfNullConfiguration() {
        try {
            TestSerdeConverter<?, ?, ?> resolved =
                    serdeManager.adapt(new Object(), null, Object.class, byte[].class,
                            TestSerdeContext.class, null);
            return "no error: " + (resolved == null ? NULL : resolved.getClass().getSimpleName());
        } catch (RuntimeException e) {
            return formatError(e);
        }
    }

    public String errorOfNullResolveConfiguration() {
        try {
            TestSerdeConverter<?, ?, ?> resolved =
                    serdeManager.resolve(null, Object.class, byte[].class, TestSerdeContext.class, null);
            return "no error: " + (resolved == null ? NULL : resolved.getClass().getSimpleName());
        } catch (RuntimeException e) {
            return formatError(e);
        }
    }

    public String errorOfNullSource() {
        try {
            TestSerdeConverter<?, ?, ?> resolved =
                    serdeManager.adapt(null, new StandardSerdeProperties(), Object.class, byte[].class,
                            TestSerdeContext.class, null);
            return "no error: " + (resolved == null ? NULL : resolved.getClass().getSimpleName());
        } catch (RuntimeException e) {
            return formatError(e);
        }
    }

    public String tryAdaptNullProps() {
        try {
            TestSerdeConverter<?, ?, ?> resolved =
                    adapterManager.tryAdapt(new Object(), null, Object.class, byte[].class,
                            TestSerdeContext.class, null);
            return "no error: " + (resolved == null ? NULL : resolved.getClass().getSimpleName());
        } catch (RuntimeException e) {
            return formatError(e);
        }
    }

    public String resolveByType(String className) {
        try {
            Object bean = beanResolver.resolve(null, Class.forName(className));
            return bean == null ? NULL : bean.getClass().getSimpleName();
        } catch (ClassNotFoundException e) {
            return formatError(new IllegalArgumentException(e.getMessage(), e));
        } catch (RuntimeException e) {
            return formatError(e);
        }
    }

    public String errorOf(String configKind, String type, String beanRef, String targetClass,
                          String objectMapperRef) {
        try {
            TestSerdeConverter<?, ?, ?> resolved =
                    resolveSerializer(config(configKind, type, beanRef, targetClass, objectMapperRef));
            return "no error: " + (resolved == null ? NULL : resolved.getClass().getSimpleName());
        } catch (RuntimeException e) {
            return formatError(e);
        }
    }

    private TestRecordProperties record(String type) {
        TestRecordProperties props = new TestRecordProperties();
        props.setType(type);
        return props;
    }

    private StandardSerdeProperties config(String configKind, String type, String beanRef, String targetClass,
                                           String objectMapperRef) {
        StandardSerdeProperties props =
                "record".equals(configKind) ? new TestRecordProperties() : new StandardSerdeProperties();
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

    private TestSerdeConverter<Object, byte[], TestSerdeContext> resolveSerializer(TestSerdeProperties props) {
        return serdeManager.resolve(props, Object.class, byte[].class, TestSerdeContext.class, null);
    }

    private TestSerdeConverter<byte[], Object, TestSerdeContext> resolveDeserializer(TestSerdeProperties props) {
        return serdeManager.resolve(props, byte[].class, Object.class, TestSerdeContext.class, null);
    }

    private TestSerdeConverter<Object, byte[], TestPlatformContext> resolvePlatformSerializer(
            TestSerdeProperties props) {
        return serdeManager.resolve(props, Object.class, byte[].class, TestPlatformContext.class, null);
    }

    private TestSerdeConverter<byte[], Object, TestPlatformContext> resolvePlatformDeserializer(
            TestSerdeProperties props) {
        return serdeManager.resolve(props, byte[].class, Object.class, TestPlatformContext.class, null);
    }

    private String formatError(RuntimeException e) {
        String message = e.getMessage() == null ? NULL
                : e.getMessage().replace("\r\n", " | ").replace("\n", " | ");
        return e.getClass().getSimpleName() + ": " + message;
    }

    private String bytesToText(byte[] data) {
        return data == null ? NULL : new String(data, StandardCharsets.UTF_8);
    }

    private String bytesToHex(byte[] data) {
        return data == null ? NULL : HEX.formatHex(data);
    }
}
