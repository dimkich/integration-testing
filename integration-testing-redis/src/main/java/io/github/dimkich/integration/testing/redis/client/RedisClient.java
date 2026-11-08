package io.github.dimkich.integration.testing.redis.client;

/**
 * Minimal Redis client used by the integration testing module.
 * <p>
 * The framework owns this connection instead of borrowing a connection factory from the
 * application under test, so the application may use any Redis driver (Jedis, Lettuce,
 * Redisson, a custom one) or no Spring Data Redis at all. Only the small command surface
 * required by the framework is exposed; {@link #execute(byte[], byte[]...)} covers arbitrary
 * commands for custom data types.
 * <p>
 * The implementation speaks RESP2 over a plain socket and supports Redis 5 and newer.
 * Instances are thread-safe: commands are serialized on the underlying connection.
 *
 * @see RedisPipeline
 */
public interface RedisClient {

    /**
     * Returns the logical connection name as configured under
     * {@code integration.testing.redis.connections}.
     */
    String getName();

    /**
     * Returns the logical database used when a key does not carry an explicit database index.
     */
    int getDefaultDatabase();

    /**
     * Executes a single command and returns the decoded reply.
     * <p>
     * Bulk strings and simple strings are returned as {@code byte[]}, integers as
     * {@link Long}, arrays as {@code Object[]} (recursively decoded), null replies as
     * {@code null}.
     *
     * @param command command name, for example {@code SET}
     * @param args    raw command arguments
     * @return decoded reply
     * @throws RedisClientException on a transport failure or a Redis error reply
     */
    Object execute(byte[] command, byte[]... args);

    /**
     * Selects the current logical database.
     *
     * @param database zero-based database index
     */
    void select(int database);

    /** Runs {@code FLUSHALL}. */
    void flushAll();

    /**
     * Runs {@code CONFIG SET}.
     *
     * @param parameter configuration parameter name
     * @param value     new value
     */
    void configSet(String parameter, String value);

    /**
     * Runs {@code PUBLISH}.
     *
     * @param channel pub/sub channel
     * @param message payload
     */
    void publish(byte[] channel, byte[] message);

    /**
     * Opens a pipeline for bulk writes.
     * <p>
     * The pipeline holds the connection until it is closed, so callers must use
     * try-with-resources. Only one pipeline may be open per client at a time; a nested pipeline
     * or a single command issued by the same thread while the pipeline is open fails with
     * {@link IllegalStateException} instead of interleaving commands and replies.
     *
     * @return pipeline bound to the current connection
     */
    RedisPipeline pipeline();

    /**
     * Closes the underlying connection; a later command reconnects lazily.
     * <p>
     * Fails with {@link IllegalStateException} when the calling thread holds an open
     * {@link RedisPipeline}; a call from another thread waits until the pipeline is closed.
     */
    void close();
}
