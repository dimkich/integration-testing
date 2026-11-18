package io.github.dimkich.integration.testing.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.moilioncircle.redis.replicator.cmd.impl.*;
import com.moilioncircle.redis.replicator.event.AbstractEvent;
import com.moilioncircle.redis.replicator.rdb.datatype.KeyValuePair;
import com.moilioncircle.redis.replicator.rdb.dump.datatype.DumpKeyValuePair;
import io.github.dimkich.integration.testing.TestConverter;
import io.github.dimkich.integration.testing.TestSetupModule;
import io.github.dimkich.integration.testing.format.common.type.TypeParser;
import io.github.dimkich.integration.testing.redis.client.RedisClient;
import io.github.dimkich.integration.testing.redis.client.RedisPipeline;
import io.github.dimkich.integration.testing.redis.config.RedisProperties;
import io.github.dimkich.integration.testing.redis.facade.*;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.zset.Aggregate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.lang.Nullable;
import redis.clients.jedis.Jedis;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Configuration
@Import({RedisReplicationMockFacade.class, RedisReplicatorSpyProcessor.class})
public class RedisTestConfig {
    /** Envelope written by {@link #binaryValueSerializer} and decoded by the test schema. */
    public static final String BINARY_VALUE_ENVELOPE = "{VER(1)}{LEN(INT, BE)}{CONTENT}";
    @Value("${embedded.redis.host}")
    private String redisHost;
    @Value("${embedded.redis.port}")
    private int redisPort;
    @Value("${embedded.redis.password:}")
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
     * Value serializer used by both the typed facade and the {@code [redisson.*]} schemas:
     * the application and the framework must serialize values identically.
     */
    @Bean
    public RedisSerializer<TestRedisDto> testValueSerializer() {
        return new Jackson2JsonRedisSerializer<>(redisObjectMapper(), TestRedisDto.class);
    }

    @Bean(destroyMethod = "close")
    public Jedis redisTestJedis() {
        Jedis jedis = new Jedis(redisHost, redisPort);
        if (!redisPassword.isEmpty()) {
            jedis.auth(redisPassword);
        }
        return jedis;
    }

    /**
     * Enables keyspace notifications and registers listeners that mimic application handlers
     * reacting to different keyspace events. The expiry listener cleans an index set, the
     * deletion listener records keys, and the multi-event listener counts notifications, so the
     * test class can wait for {@code onMessage} with
     * {@link io.github.dimkich.integration.testing.redis.RedisKeyspaceEventsAwait} and then assert
     * the number of handled notifications.
     */
    @Bean
    public RedisMessageListenerContainer keyspaceListenerContainer(
            RedisConnectionFactory redissonConnectionFactory,
            StringRedisTemplate keyspaceStringRedisTemplate) {
        try (Jedis jedis = new Jedis(redisHost, redisPort)) {
            if (!redisPassword.isEmpty()) {
                jedis.auth(redisPassword);
            }
            jedis.configSet("notify-keyspace-events", "KEA");
        }
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redissonConnectionFactory);
        container.addMessageListener(new ExpiredKeyspaceListener(keyspaceStringRedisTemplate),
                new PatternTopic("__keyevent@*__:expired"));
        container.addMessageListener(new DeletedKeyspaceListener(keyspaceStringRedisTemplate),
                new PatternTopic("__keyevent@*__:del"));
        container.addMessageListener(new EventKeyspaceListener(keyspaceStringRedisTemplate),
                List.of(new PatternTopic("__keyevent@*__:set"),
                        new PatternTopic("__keyevent@*__:hset"),
                        new PatternTopic("__keyevent@*__:lpush"),
                        new PatternTopic("__keyevent@*__:sadd"),
                        new PatternTopic("__keyevent@*__:zadd"),
                        new PatternTopic("__keyevent@*__:expire"),
                        new PatternTopic("__keyevent@*__:persist"),
                        new PatternTopic("__keyevent@*__:rename_from"),
                        new PatternTopic("__keyevent@*__:rename_to")));
        return container;
    }

    @Bean
    public StringRedisTemplate keyspaceStringRedisTemplate(RedisConnectionFactory redissonConnectionFactory) {
        return new StringRedisTemplate(redissonConnectionFactory);
    }

    @Bean
    public RedisTestFacade<String, TestRedisDto> redissonTestFacade(
            Jedis redisTestJedis,
            @Qualifier("testValueSerializer") RedisSerializer<TestRedisDto> testValueSerializer) {
        return new RedisTestFacade<>(redisTestJedis, new StringRedisSerializer(), testValueSerializer,
                jdkSerializer(), jdkSerializer());
    }

    /**
     * {@code RedisTemplate} defaults hash keys and values to {@link JdkSerializationRedisSerializer},
     * so the facade must use the same serializers for hash and stream fields.
     */
    private static RedisSerializer<Object> jdkSerializer() {
        return new JdkSerializationRedisSerializer();
    }

    /**
     * Entry point for the {@code framework client} XML container. Both framework clients are
     * registered by {@code RedisConfig.PostProcessor} as {@code <connection>RedisClient}.
     */
    @Bean
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public RedisClientTestFacade redisClientTestFacade(
            @Qualifier("redissonConnectionFactoryRedisClient") RedisClient redissonClient,
            @Qualifier("jedisConnectionFactoryRedisClient") RedisClient jedisClient,
            RedisProperties redisProperties) {
        Map<String, RedisClient> clients = new LinkedHashMap<>();
        clients.put("redissonConnectionFactory", redissonClient);
        clients.put("jedisConnectionFactory", jedisClient);
        return new RedisClientTestFacade(clients, redisProperties);
    }

    /**
     * Serializer that writes enveloped bytes, emulating an application that stores values with
     * a binary envelope. The matching test schema uses
     * {@code value: {bean-ref: testValueSerializer, binary-envelope: ...}} so that the
     * storage decodes the envelope through the core serde pipeline.
     */
    @Bean
    public EnvelopedRedisSerializer binaryValueSerializer(SerdeManager serdeManager) {
        StandardSerdeProperties props = new StandardSerdeProperties();
        props.setBeanRef("testValueSerializer");
        props.setBinaryEnvelope(BINARY_VALUE_ENVELOPE);
        TestSerdeConverter<Object, ByteBuffer, TestSerdeContext> envelopedSerializer =
                serdeManager.resolve(props, Object.class, ByteBuffer.class, TestSerdeContext.class, null);
        TestSerdeConverter<ByteBuffer, Object, TestSerdeContext> envelopedDeserializer =
                serdeManager.resolve(props, ByteBuffer.class, Object.class, TestSerdeContext.class, null);
        return new EnvelopedRedisSerializer(envelopedSerializer, envelopedDeserializer);
    }

    @Bean
    public RedisTestFacade<String, TestRedisDto> binaryTestFacade(
            Jedis redisTestJedis, EnvelopedRedisSerializer binaryValueSerializer) {
        return new RedisTestFacade<>(redisTestJedis, new StringRedisSerializer(), binaryValueSerializer,
                jdkSerializer(), jdkSerializer());
    }

    @Bean
    public RedisTestFacade<String, String> redisStringFacade(Jedis redisTestJedis) {
        return new RedisTestFacade<>(redisTestJedis, new StringRedisSerializer(), new StringRedisSerializer(),
                new StringRedisSerializer(), new StringRedisSerializer());
    }

    @Bean
    public RedisPushTestFacade redisPushTestFacade(RedisConnectionFactory redissonConnectionFactory,
                                                   RedisConnectionFactory jedisConnectionFactory,
                                                   @Qualifier("testValueSerializer")
                                                   RedisSerializer<TestRedisDto> testValueSerializer) {
        return new RedisPushTestFacade(redissonConnectionFactory, jedisConnectionFactory, testValueSerializer);
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
    public RedisTestFacade<String, String> jedisFacade(Jedis redisTestJedis) {
        return new RedisTestFacade<>(redisTestJedis, new StringRedisSerializer(), new StringRedisSerializer(),
                new StringRedisSerializer(), new StringRedisSerializer());
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
        return new TestSetupModule().addSubTypes(DumpKeyValuePair.class, TestRedisDto.class,
                        RedisComplexKey.class, InvalidDataAccessApiUsageException.class,
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
                        SetExCommand.class, PSetExCommand.class, GetSetCommand.class, PingCommand.class,
                        SPublishCommand.class)
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
    public RedisClient snapshotTestFactoryRedisClient() {
        RedisClient client = mock(RedisClient.class);
        RedisPipeline pipeline = mock(RedisPipeline.class);
        when(client.pipeline()).thenReturn(pipeline);
        when(client.getDefaultDatabase()).thenReturn(0);
        return client;
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
    public static class EnvelopedRedisSerializer implements RedisSerializer<TestRedisDto> {
        private final TestSerdeConverter<Object, ByteBuffer, TestSerdeContext> serializer;
        private final TestSerdeConverter<ByteBuffer, Object, TestSerdeContext> deserializer;

        @Override
        public byte[] serialize(TestRedisDto value) {
            return TestSerdeConverter.toByteArray(serializer.convert(value, TestSerdeContext.EMPTY));
        }

        @Override
        public TestRedisDto deserialize(byte[] bytes) {
            return (TestRedisDto) deserializer.convert(bytes == null ? null : ByteBuffer.wrap(bytes),
                    TestSerdeContext.EMPTY);
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

    /**
     * Mimics an application listener that cleans index sets when a key expires. The listener
     * removes the expired key from {@code redisson.expired.index}; keys outside the
     * {@code redisson.expired.} prefix are ignored.
     */
    @RequiredArgsConstructor
    public static class ExpiredKeyspaceListener implements MessageListener {
        private static final String PREFIX = "redisson.expired.";
        private static final String INDEX_KEY = "redisson.expired.index";

        private final StringRedisTemplate template;

        @Override
        public void onMessage(Message message, @Nullable byte[] pattern) {
            String key = new String(message.getBody(), StandardCharsets.UTF_8);
            if (key.startsWith(PREFIX)) {
                template.opsForSet().remove(INDEX_KEY, key);
            }
        }
    }

    /** Records deleted keys with the {@code redisson.await.del.} prefix in a signal list. */
    @RequiredArgsConstructor
    public static class DeletedKeyspaceListener implements MessageListener {
        private static final String PREFIX = "redisson.await.del.";
        private static final String RECEIVED_KEY = "redisson.await.del.received";

        private final StringRedisTemplate template;

        @Override
        public void onMessage(Message message, @Nullable byte[] pattern) {
            String key = new String(message.getBody(), StandardCharsets.UTF_8);
            if (key.startsWith(PREFIX)) {
                template.opsForList().rightPush(RECEIVED_KEY, key);
            }
        }
    }

    /**
     * Counts notifications for keys with the given prefix. A counter is used instead of a
     * signal list because the listener container dispatches messages concurrently, so the
     * order of {@code onMessage} calls is not guaranteed.
     */
    private static void countKeyspaceEvent(StringRedisTemplate template, Message message,
                                           String prefix, String counterKey) {
        String key = new String(message.getBody(), StandardCharsets.UTF_8);
        if (key.startsWith(prefix)) {
            template.opsForValue().increment(counterKey);
        }
    }

    /**
     * Counts keyspace notifications of all awaited types. One listener serves all event types,
     * so the test needs only one instrumented pointcut and one subscription.
     */
    @RequiredArgsConstructor
    public static class EventKeyspaceListener implements MessageListener {
        private static final Map<String, EventCounter> EVENTS = Map.ofEntries(
                Map.entry("set", new EventCounter("redisson.await.set.",
                        "redisson.await.keyspace.set.count")),
                Map.entry("hset", new EventCounter("redisson.await.hset.",
                        "redisson.await.keyspace.hset.count")),
                Map.entry("lpush", new EventCounter("redisson.await.lpush.",
                        "redisson.await.keyspace.lpush.count")),
                Map.entry("sadd", new EventCounter("redisson.await.sadd.",
                        "redisson.await.keyspace.sadd.count")),
                Map.entry("zadd", new EventCounter("redisson.await.zadd.",
                        "redisson.await.keyspace.zadd.count")),
                Map.entry("expire", new EventCounter("redisson.await.expire.",
                        "redisson.await.keyspace.expire.count")),
                Map.entry("persist", new EventCounter("redisson.await.persist.",
                        "redisson.await.keyspace.persist.count")),
                Map.entry("rename_from", new EventCounter("redisson.await.rename.",
                        "redisson.await.keyspace.rename.from.count")),
                Map.entry("rename_to", new EventCounter("redisson.await.rename.",
                        "redisson.await.keyspace.rename.to.count"))
        );

        private final StringRedisTemplate template;

        @Override
        public void onMessage(Message message, @Nullable byte[] pattern) {
            String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
            String type = channel.substring(channel.lastIndexOf(':') + 1);
            EventCounter event = EVENTS.get(type);
            if (event != null) {
                countKeyspaceEvent(template, message, event.prefix(), event.counterKey());
            }
        }

        private record EventCounter(String prefix, String counterKey) {
        }
    }

}
