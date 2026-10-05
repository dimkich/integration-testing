package io.github.dimkich.integration.testing.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.moilioncircle.redis.replicator.cmd.impl.*;
import com.moilioncircle.redis.replicator.event.AbstractEvent;
import com.moilioncircle.redis.replicator.rdb.datatype.KeyValuePair;
import io.github.dimkich.integration.testing.TestConverter;
import io.github.dimkich.integration.testing.TestSetupModule;
import io.github.dimkich.integration.testing.format.common.type.TypeParser;
import io.github.dimkich.integration.testing.redis.facade.RedisReplicationMockFacade;
import io.github.dimkich.integration.testing.redis.facade.RedisReplicatorSpyProcessor;
import io.github.dimkich.integration.testing.redis.facade.RedisSerdeTestFacade;
import io.github.dimkich.integration.testing.redis.facade.RedisTestFacade;
import io.github.dimkich.integration.testing.redis.jackson.*;
import io.github.dimkich.integration.testing.redis.registry.RedisDataSchemaRegistry;
import io.github.dimkich.integration.testing.serde.SerdeManager;
import io.github.dimkich.integration.testing.serde.StandardSerdeProperties;
import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.BaseCodec;
import org.redisson.client.codec.Codec;
import org.redisson.client.protocol.Decoder;
import org.redisson.client.protocol.Encoder;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.zset.Aggregate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@Configuration
@Import({RedisReplicationMockFacade.class, RedisReplicatorSpyProcessor.class})
public class RedisTestConfig {
    /** Envelope written by {@link #binaryTestRedisTemplate} and decoded by the test schema. */
    public static final String BINARY_VALUE_ENVELOPE = "{VER(1)}{LEN(INT, BE)}{CONTENT}";
    @Value("${spring.data.redis.host}")
    private String redisHost;
    @Value("${spring.data.redis.port}")
    private int redisPort;
    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        org.redisson.config.Config config = new org.redisson.config.Config();
        String address = String.format("redis://%s:%d", redisHost, redisPort);
        config.useSingleServer()
                .setAddress(address)
                .setPassword(redisPassword.isEmpty() ? null : redisPassword);
        return Redisson.create(config);
    }

    @Bean
    public RedisConnectionFactory redissonConnectionFactory(RedissonClient redissonClient) {
        return new RedissonConnectionFactory(redissonClient);
    }

    /**
     * Value serializer used by both {@link #testRedisTemplate} and the {@code [redisson.*]}
     * schemas: the application and the framework must serialize values identically.
     */
    @Bean
    public RedisSerializer<TestRedisDto> testValueSerializer() {
        return new Jackson2JsonRedisSerializer<>(redisObjectMapper(), TestRedisDto.class);
    }

    @Bean
    public RedisTemplate<String, TestRedisDto> testRedisTemplate(RedisConnectionFactory redissonConnectionFactory,
                                                                 RedisSerializer<TestRedisDto> testValueSerializer) {
        RedisTemplate<String, TestRedisDto> template = new RedisTemplate<>();
        template.setConnectionFactory(redissonConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(testValueSerializer);
        return template;
    }

    @Bean
    public RedisTestFacade<String, TestRedisDto> redissonTestFacade(RedisTemplate<String, TestRedisDto> testRedisTemplate) {
        return new RedisTestFacade<>(testRedisTemplate);
    }

    /**
     * Template whose value serializer writes enveloped bytes, emulating an application
     * that stores values with a binary envelope. The matching test schema uses
     * {@code value: {bean-ref: testValueSerializer, binary-envelope: ...}} so that the
     * storage decodes the envelope through the core serde pipeline.
     */
    @Bean
    public RedisTemplate<String, TestRedisDto> binaryTestRedisTemplate(RedisConnectionFactory redissonConnectionFactory,
                                                                       RedisSerializer<TestRedisDto> testValueSerializer,
                                                                       SerdeManager serdeManager) {
        RedisTemplate<String, TestRedisDto> template = new RedisTemplate<>();
        template.setConnectionFactory(redissonConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        StandardSerdeProperties props = new StandardSerdeProperties();
        props.setBeanRef("testValueSerializer");
        props.setBinaryEnvelope(BINARY_VALUE_ENVELOPE);
        TestSerdeConverter<Object, byte[], TestSerdeContext> envelopedSerializer =
                serdeManager.resolve(props, Object.class, byte[].class, TestSerdeContext.class, null);
        TestSerdeConverter<byte[], Object, TestSerdeContext> envelopedDeserializer =
                serdeManager.resolve(props, byte[].class, Object.class, TestSerdeContext.class, null);
        template.setValueSerializer(new EnvelopedRedisSerializer(envelopedSerializer, envelopedDeserializer));
        return template;
    }

    @Bean
    public RedisTestFacade<String, TestRedisDto> binaryTestFacade(
            RedisTemplate<String, TestRedisDto> binaryTestRedisTemplate) {
        return new RedisTestFacade<>(binaryTestRedisTemplate);
    }

    @Bean
    public RedisTestFacade<String, String> redisStringFacade(RedisConnectionFactory redissonConnectionFactory) {
        return new RedisTestFacade<>(new StringRedisTemplate(redissonConnectionFactory));
    }

    @Bean
    public RedisConnectionFactory jedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
        if (!redisPassword.isEmpty()) {
            config.setPassword(redisPassword);
        }
        return new JedisConnectionFactory(config);
    }

    @Bean
    public StringRedisTemplate jedisRedisTemplate(RedisConnectionFactory jedisConnectionFactory) {
        return new StringRedisTemplate(jedisConnectionFactory);
    }

    @Bean
    public RedisTestFacade<String, String> jedisFacade(StringRedisTemplate jedisRedisTemplate) {
        return new RedisTestFacade<>(jedisRedisTemplate);
    }

    /**
     * Single-slot Spring Data serializer bean used by the {@code Serde} test container as a
     * component source.
     */
    @Bean
    public RedisSerializer<String> testStringSerializer() {
        return new StringRedisSerializer();
    }

    /** Redisson codec bean used by the {@code Serde} test container as a component source. */
    @Bean
    public Codec redissonJsonCodec() {
        return new JsonJacksonCodec();
    }

    /**
     * Redisson codec whose value, map key and map value slots encode with different prefixes,
     * so tests can prove that a schema slot uses its own encoder/decoder pair.
     */
    @Bean
    public Codec redissonSlotCodec() {
        return new SlotPrefixCodec();
    }

    /** Bean that only exposes a Redisson codec via {@code getCodec()}, like {@code Config}. */
    @Bean
    public RedissonCodecHolder redissonCodecHolder(Codec redissonJsonCodec) {
        return new RedissonCodecHolder(redissonJsonCodec);
    }

    /** Bean whose {@code getCodec()} fails, used to verify that the real cause is not swallowed. */
    @Bean
    public BrokenCodecHolder brokenCodecHolder() {
        return new BrokenCodecHolder();
    }

    /** Bean whose {@code getCodec()} returns a value that is not a {@link Codec}. */
    @Bean
    public NotACodecHolder notACodecHolder() {
        return new NotACodecHolder();
    }

    /** Bean whose {@code getCodec()} fails with a checked exception. */
    @Bean
    public CheckedCodecHolder checkedCodecHolder() {
        return new CheckedCodecHolder();
    }

    @Bean
    public RedisSerdeTestFacade redisSerdeTestFacade(SerdeManager serdeManager,
                                                     RedisDataSchemaRegistry schemaRegistry, TypeParser typeParser) {
        return new RedisSerdeTestFacade(serdeManager, schemaRegistry, typeParser);
    }

    @Bean
    TestSetupModule module() {
        return new TestSetupModule().addSubTypes(TestRedisDto.class, InvalidDataAccessApiUsageException.class,
                        RedisListCommands.Direction.class, Aggregate.class, RedisStringCommands.BitOperation.class,
                        BitFieldSubCommands.class, BitFieldSubCommands.BitFieldGet.class,
                        BitFieldSubCommands.BitFieldSet.class, BitFieldSubCommands.BitFieldIncrBy.class,
                        BitFieldSubCommands.BitFieldType.class, RestoreCommand.class)
                .addParentType(BitFieldSubCommands.BitFieldSubCommand.class)
                .addAlias(BitFieldSubCommands.Offset.class, "BitFieldOffset")
                .addSubTypes("com.moilioncircle.redis.replicator.event")
                .addSubTypes("com.moilioncircle.redis.replicator.rdb.datatype")
                .addSubTypes("com.moilioncircle.redis.replicator.rdb.iterable.datatype")
                .addSubTypes(XDelExCommand.class, "XDelExCommand")
                .addSubTypes(XSetIdCommand.class, "XSetIdCommand")
                .addSubTypes(ExpireCommand.class, ExpireAtCommand.class, PExpireCommand.class,
                        SetExCommand.class, PSetExCommand.class, GetSetCommand.class, PingCommand.class)
                .addJacksonModule(new SimpleModule()
                        .setMixInAnnotation(AbstractEvent.class, AbstractEventMixIn.class)
                        .setMixInAnnotation(KeyValuePair.class, AbstractEventMixIn.class)
                        .setMixInAnnotation(com.moilioncircle.redis.replicator.rdb.datatype.Stream.class,
                                StreamMixIn.class)
                        .setMixInAnnotation(com.moilioncircle.redis.replicator.rdb.datatype.Stream.Entry.class,
                                StreamEntryMixIn.class)
                        .setMixInAnnotation(BitFieldSubCommands.class, BitFieldSubCommandsMixin.class)
                        .setMixInAnnotation(BitFieldSubCommands.BitFieldGet.class, BitFieldGetMixin.class)
                        .setMixInAnnotation(BitFieldSubCommands.BitFieldSet.class, BitFieldSetMixin.class)
                        .setMixInAnnotation(BitFieldSubCommands.BitFieldIncrBy.class, BitFieldIncrByMixIn.class)
                        .setMixInAnnotation(BitFieldSubCommands.BitFieldType.class, BitFieldTypeMixin.class)
                        .setMixInAnnotation(BitFieldSubCommands.Offset.class, OffsetMixin.class)
                );
    }

    @Bean
    TestConverter exceptionMessageNormalizer() {
        return test -> {
            if (test.getResponse() instanceof Throwable t) {
                String msg = t.getMessage();
                if (msg != null && msg.contains("channel: [id:")) {
                    String cleanMessage = msg.split("\\. channel:")[0];
                    try {
                        java.lang.reflect.Field field = Throwable.class.getDeclaredField("detailMessage");
                        field.setAccessible(true);
                        field.set(t, cleanMessage);
                    } catch (Exception ignored) {
                    }
                }
            }
        };
    }

    @Bean
    ObjectMapper redisObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }

    @Bean
    public RedisConnectionFactory snapshotTestFactory() {
        RedisConnectionFactory factory = mock(RedisConnectionFactory.class);
        RedisConnection connection = mock(RedisConnection.class);
        RedisServerCommands serverCommands = mock(RedisServerCommands.class);
        doReturn(connection).when(factory).getConnection();
        doReturn(serverCommands).when(connection).serverCommands();

        return factory;
    }

    @Bean
    TestConverter nameSetter() {
        return tc -> {
            if (tc.getBean() != null && tc.getBean().equals("redisReplicationMockFacade")) {
                tc.setName(tc.getRequest().get(0).getClass().getSimpleName());
            }
        };
    }

    /**
     * Spring Data {@link RedisSerializer} that delegates to core enveloped serializer
     * and deserializer, so the template reads and writes bytes exactly as the schema expects.
     */
    @RequiredArgsConstructor
    static class EnvelopedRedisSerializer implements RedisSerializer<TestRedisDto> {
        private final TestSerdeConverter<Object, byte[], TestSerdeContext> serializer;
        private final TestSerdeConverter<byte[], Object, TestSerdeContext> deserializer;

        @Override
        public byte[] serialize(TestRedisDto value) {
            return serializer.convert(value, TestSerdeContext.EMPTY);
        }

        @Override
        public TestRedisDto deserialize(byte[] bytes) {
            return (TestRedisDto) deserializer.convert(bytes, TestSerdeContext.EMPTY);
        }
    }

    /** Redisson codec with distinguishable value/map-key/map-value slots. */
    public static class SlotPrefixCodec extends BaseCodec {

        @Override
        public Encoder getValueEncoder() {
            return object -> encode("V:", object);
        }

        @Override
        public Decoder<Object> getValueDecoder() {
            return (buffer, state) -> decode("V:", buffer);
        }

        @Override
        public Encoder getMapKeyEncoder() {
            return object -> encode("K:", object);
        }

        @Override
        public Decoder<Object> getMapKeyDecoder() {
            return (buffer, state) -> decode("K:", buffer);
        }

        @Override
        public Encoder getMapValueEncoder() {
            return object -> encode("W:", object);
        }

        @Override
        public Decoder<Object> getMapValueDecoder() {
            return (buffer, state) -> decode("W:", buffer);
        }

        private static ByteBuf encode(String prefix, Object object) {
            return Unpooled.wrappedBuffer((prefix + object).getBytes(StandardCharsets.UTF_8));
        }

        private static Object decode(String prefix, ByteBuf buffer) {
            String value = buffer.toString(StandardCharsets.UTF_8);
            if (!value.startsWith(prefix)) {
                throw new IllegalStateException(String.format(
                        "Unexpected slot value [%s]: expected the [%s] prefix", value, prefix));
            }
            return value.substring(prefix.length());
        }
    }

    /** Test stand-in for beans that expose a Redisson codec only via {@code getCodec()}. */
    @Getter
    @RequiredArgsConstructor
    public static class RedissonCodecHolder {
        private final Codec codec;
    }

    /** Test stand-in for a bean whose {@code getCodec()} fails. */
    public static class BrokenCodecHolder {
        public Codec getCodec() {
            throw new IllegalStateException("codec is not initialized");
        }
    }

    /** Test stand-in for a bean whose {@code getCodec()} is not a codec. */
    public static class NotACodecHolder {
        public Object getCodec() {
            return "not a codec";
        }
    }

    /** Test stand-in for a bean whose {@code getCodec()} fails with a checked exception. */
    public static class CheckedCodecHolder {
        public Codec getCodec() throws Exception {
            throw new Exception("codec lookup failed");
        }
    }
}