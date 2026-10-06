package io.github.dimkich.integration.testing.redis.message;

import java.nio.ByteBuffer;

/**
 * Identity of a Redis Pub/Sub push as it is observed on the transport: the connection name,
 * the channel and the raw payload bytes.
 * <p>
 * The connection prevents matches between different Redis instances and is required because
 * one test application may talk to several servers. {@link ByteBuffer} is used instead of
 * {@code byte[]} because it compares by content, so an identity produced by
 * {@link RedisPushSender} equals the identity of the same push captured by
 * {@link RedisPushCapture}.
 *
 * @param connection connection factory bean name the push was observed on
 * @param channel    pub/sub channel
 * @param payload    raw payload bytes
 */
public record RedisPushIdentity(String connection, String channel, ByteBuffer payload) {
}
