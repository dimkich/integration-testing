package io.github.dimkich.integration.testing.redis.message;

import io.github.dimkich.integration.testing.message.AbstractMessage;
import io.github.dimkich.integration.testing.message.TestMessageSender;
import io.github.dimkich.integration.testing.redis.registry.RedisDataSchemaRegistry;
import io.github.dimkich.integration.testing.redis.serde.RedisDataCodec;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.nio.ByteBuffer;

/**
 * Test message sender that publishes {@link RedisPush} messages to Redis using the key codec
 * for the channel name and the value codec configured for the target channel.
 * <p>
 * One sender is registered per {@link RedisConnectionFactory}
 * ({@link io.github.dimkich.integration.testing.redis.config.RedisConfig}). The message is
 * published through a live connection, and the identity of the written bytes is returned so
 * that {@link io.github.dimkich.integration.testing.message.TestMessages} can filter out the
 * captured echo.
 *
 * @see RedisPushCapture
 */
@Slf4j
@RequiredArgsConstructor
public class RedisPushSender implements TestMessageSender {
    private final String connectionName;
    private final RedisConnectionFactory connectionFactory;
    private final RedisDataSchemaRegistry schemaRegistry;

    @Override
    public boolean canSend(AbstractMessage message) {
        return message instanceof RedisPush push && connectionName.equals(push.getConnection());
    }

    @Override
    public Object sendInboundMessage(AbstractMessage message) {
        if (!(message instanceof RedisPush push)) {
            throw new IllegalArgumentException("Expected RedisPush, got " + message.getClass());
        }

        String channel = push.getChannel();
        RedisDataCodec keyCodec = schemaRegistry.findKeyCodec(connectionName);
        RedisDataCodec valueCodec = schemaRegistry.findSchema(connectionName, channel).getSchema().getValueCodec();
        byte[] channelRaw = keyCodec.serialize(channel);
        byte[] payload = valueCodec.serialize(push.getValue());

        log.debug("Publishing push message to channel [{}] via connection [{}]", channel, connectionName);

        @Cleanup RedisConnection connection = connectionFactory.getConnection();
        connection.publish(channelRaw, payload);
        return new RedisPushIdentity(connectionName, channel, ByteBuffer.wrap(payload));
    }
}
