package io.github.dimkich.integration.testing.redis.facade;

import io.github.dimkich.integration.testing.redis.TestRedisDto;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Test stand-in for a system under test that both publishes and consumes Redis Pub/Sub
 * messages.
 * <p>
 * Two listeners emulate the SUT side: a pattern subscription for string pushes and a channel
 * subscription for typed {@link TestRedisDto} pushes. The {@code publish*} methods emulate the
 * SUT publishing a push, and {@code await*} methods block until a push sent by the framework
 * reaches the SUT. Payloads are deserialized manually because the listener container passes
 * raw body bytes.
 */
@Slf4j
public class RedisPushTestFacade {
    private static final String STRING_CHANNEL_PATTERN = "redisson.push.*";
    private static final String DTO_CHANNEL = "redisson.TestRedisDto.push";
    private static final long AWAIT_TIMEOUT_MS = 5000;

    private final RedisConnectionFactory connectionFactory;
    private final RedisConnectionFactory rawExecuteConnectionFactory;
    private final RedisSerializer<TestRedisDto> dtoSerializer;
    private final RedisMessageListenerContainer stringListenerContainer = new RedisMessageListenerContainer();
    private final RedisMessageListenerContainer dtoListenerContainer = new RedisMessageListenerContainer();
    private final BlockingQueue<String> stringMessages = new LinkedBlockingQueue<>();
    private final BlockingQueue<TestRedisDto> dtoMessages = new LinkedBlockingQueue<>();

    public RedisPushTestFacade(RedisConnectionFactory connectionFactory,
                               RedisConnectionFactory rawExecuteConnectionFactory,
                               RedisSerializer<TestRedisDto> dtoSerializer) {
        this.connectionFactory = connectionFactory;
        this.rawExecuteConnectionFactory = rawExecuteConnectionFactory;
        this.dtoSerializer = dtoSerializer;
    }

    @PostConstruct
    public void start() {
        stringListenerContainer.setConnectionFactory(connectionFactory);
        stringListenerContainer.addMessageListener(this::onStringMessage, new PatternTopic(STRING_CHANNEL_PATTERN));
        stringListenerContainer.afterPropertiesSet();
        stringListenerContainer.start();

        dtoListenerContainer.setConnectionFactory(connectionFactory);
        dtoListenerContainer.addMessageListener(this::onDtoMessage, new ChannelTopic(DTO_CHANNEL));
        dtoListenerContainer.afterPropertiesSet();
        dtoListenerContainer.start();

        log.debug("Push test facade subscribed to [{}] and [{}]", STRING_CHANNEL_PATTERN, DTO_CHANNEL);
    }

    @PreDestroy
    @SneakyThrows
    public void stop() {
        stringListenerContainer.stop();
        stringListenerContainer.destroy();
        dtoListenerContainer.stop();
        dtoListenerContainer.destroy();
    }

    private void onStringMessage(Message message, byte[] pattern) {
        stringMessages.add(new String(message.getBody(), StandardCharsets.UTF_8));
    }

    private void onDtoMessage(Message message, byte[] pattern) {
        TestRedisDto dto = dtoSerializer.deserialize(message.getBody());
        if (dto != null) {
            dtoMessages.add(dto);
        }
    }

    /** Emulates the SUT publishing a string push. */
    public void publishString(String channel, String value) {
        publish(channel, value.getBytes(StandardCharsets.UTF_8));
    }

    /** Emulates the SUT publishing a typed push. */
    public void publishDto(String channel, TestRedisDto value) {
        publish(channel, dtoSerializer.serialize(value));
    }

    /** Publishes a sharded push (SPUBLISH), which the framework should capture via replication. */
    public void sPublishString(String channel, String value) {
        @Cleanup RedisConnection connection = rawExecuteConnectionFactory.getConnection();
        Object result = connection.execute("SPUBLISH",
                channel.getBytes(StandardCharsets.UTF_8), value.getBytes(StandardCharsets.UTF_8));
        log.debug("SPUBLISH [{}] -> {}", channel, result);
    }

    /** Waits until the SUT receives a string push, or returns {@code null} on timeout. */
    public String awaitString() throws InterruptedException {
        return stringMessages.poll(AWAIT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

    /** Waits until the SUT receives a typed push, or returns {@code null} on timeout. */
    public TestRedisDto awaitDto() throws InterruptedException {
        return dtoMessages.poll(AWAIT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

    /** Drops everything received by the SUT so far. */
    public void clear() {
        stringMessages.clear();
        dtoMessages.clear();
    }

    private void publish(String channel, byte[] payload) {
        @Cleanup RedisConnection connection = connectionFactory.getConnection();
        connection.publish(channel.getBytes(StandardCharsets.UTF_8), payload);
    }
}
