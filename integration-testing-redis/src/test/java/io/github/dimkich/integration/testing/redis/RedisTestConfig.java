package io.github.dimkich.integration.testing.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.moilioncircle.redis.replicator.cmd.impl.RestoreCommand;
import com.moilioncircle.redis.replicator.cmd.impl.XDelExCommand;
import com.moilioncircle.redis.replicator.cmd.impl.XSetIdCommand;
import com.moilioncircle.redis.replicator.event.AbstractEvent;
import com.moilioncircle.redis.replicator.rdb.datatype.KeyValuePair;
import io.github.dimkich.integration.testing.TestConverter;
import io.github.dimkich.integration.testing.TestSetupModule;
import io.github.dimkich.integration.testing.redis.facade.BinaryLayoutTestFacade;
import io.github.dimkich.integration.testing.redis.facade.RedisReplicationMockFacade;
import io.github.dimkich.integration.testing.redis.facade.RedisReplicatorSpyProcessor;
import io.github.dimkich.integration.testing.redis.facade.RedisTestFacade;
import io.github.dimkich.integration.testing.redis.jackson.*;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
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
import org.springframework.data.redis.serializer.StringRedisSerializer;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@Configuration
@Import({BinaryLayoutTestFacade.class, RedisReplicationMockFacade.class, RedisReplicatorSpyProcessor.class})
public class RedisTestConfig {
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

    @Bean
    public RedisTemplate<String, TestRedisDto> testRedisTemplate(RedisConnectionFactory redissonConnectionFactory) {
        RedisTemplate<String, TestRedisDto> template = new RedisTemplate<>();
        template.setConnectionFactory(redissonConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(redisObjectMapper(), TestRedisDto.class));
        return template;
    }

    @Bean
    public RedisTestFacade<String, TestRedisDto> redissonTestFacade(RedisTemplate<String, TestRedisDto> testRedisTemplate) {
        return new RedisTestFacade<>(testRedisTemplate);
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
}
