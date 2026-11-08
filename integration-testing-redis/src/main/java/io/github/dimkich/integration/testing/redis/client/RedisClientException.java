package io.github.dimkich.integration.testing.redis.client;

/**
 * Unchecked failure of a Redis command or connection.
 * <p>
 * Thrown by {@link RedisClient} when the transport fails, the server replies with an error,
 * or the connection is misconfigured. The message always contains the connection name and the
 * command that failed.
 */
public class RedisClientException extends RuntimeException {

    public RedisClientException(String message) {
        super(message);
    }

    public RedisClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
