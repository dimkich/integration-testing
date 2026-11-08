package io.github.dimkich.integration.testing.redis.replication;

import io.github.dimkich.integration.testing.redis.client.RedisClient;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Pub/sub handshake that blocks test code until a synthetic {@code PUBLISH} has been replicated
 * into the in-memory store.
 * <p>
 * {@link #triggerAndAwait(long)} publishes a unique token on {@link #SYNC_CHANNEL} via the
 * framework connection. The replicator eventually emits a matching {@code PublishCommand};
 * stream handlers forward it to {@link #release(byte[], byte[])}, which completes the wait. That
 * proves the replication stream has caught up to at least the barrier command—used by
 * {@link io.github.dimkich.integration.testing.redis.RedisWaitCompletion} after initialization
 * and after the test action.
 * <p>
 * The barrier only provides the round trip: callers must ensure live command sync is running
 * (see {@link RedisSyncState#awaitActive(long)}) before triggering. When replication fails,
 * {@link RedisSyncState#setFatalError(Throwable)} invokes the registered
 * {@link RedisSyncState#onFatalError(java.util.function.Consumer) failure handler}, which aborts
 * an in-flight wait via {@link #fail(Throwable)}.
 *
 * @see io.github.dimkich.integration.testing.redis.replication.handler.stream.PublishCommandHandler
 */
@Slf4j
public class RedisSyncBarrier {
    /** Redis channel used for barrier tokens; must not collide with application pub/sub. */
    public static final String SYNC_CHANNEL = "__integration_testing_sync__";

    private static final byte[] SYNC_CHANNEL_BYTES = SYNC_CHANNEL.getBytes(StandardCharsets.UTF_8);

    private final String name;
    private final RedisClient client;
    private final RedisSyncState state;
    private final AtomicReference<byte[]> expectedToken = new AtomicReference<>();
    private final AtomicReference<CompletableFuture<Void>> syncFuture = new AtomicReference<>();
    private final AtomicLong tokenSequence = new AtomicLong(0);

    /**
     * @param name   connection name used in tokens and error messages
     * @param client connection used to publish barrier tokens
     * @param state  synchronization state whose fatal errors abort in-flight waits
     */
    public RedisSyncBarrier(String name, RedisClient client, RedisSyncState state) {
        this.name = name;
        this.client = client;
        this.state = state;
        state.onFatalError(this::fail);
    }

    /**
     * Publishes a barrier token and waits until replication delivers the same token back, or times out.
     *
     * @param timeoutMs maximum wait in milliseconds
     * @throws RuntimeException if a fatal replication error is pending, the wait times out, or is
     *                          interrupted
     */
    public void triggerAndAwait(long timeoutMs) {
        state.throwFatalError();
        CompletableFuture<Void> future = new CompletableFuture<>();
        syncFuture.set(future);

        String token = name + ":" + tokenSequence.incrementAndGet();
        byte[] tokenBytes = token.getBytes(StandardCharsets.UTF_8);
        expectedToken.set(tokenBytes);

        try {
            client.publish(SYNC_CHANNEL_BYTES, tokenBytes);
            log.debug("Sync barrier (PUBLISH) triggered for {} with token {}", name, token);
            future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Redis Sync Interrupted for [" + name + "] using PUBLISH", e);
        } catch (TimeoutException | ExecutionException e) {
            throw new RuntimeException("Redis Sync Timeout/Error for [" + name + "] using PUBLISH", e);
        } finally {
            syncFuture.set(null);
            expectedToken.set(null);
        }
    }

    /**
     * Completes a pending {@link #triggerAndAwait(long)} when the replicated publish matches the
     * expected channel and token.
     *
     * @param channel pub/sub channel from the replicated {@code PUBLISH}
     * @param message message body (barrier token)
     */
    public void release(byte[] channel, byte[] message) {
        if (isSyncChannel(channel) && Arrays.equals(message, expectedToken.get())) {
            CompletableFuture<Void> future = syncFuture.get();
            if (future != null) {
                future.complete(null);
                log.debug("Sync barrier released for {} via PUBLISH token", name);
            }
        }
    }

    /**
     * Checks whether the given raw channel is the barrier channel. Used by stream processing to
     * keep barrier traffic out of captured Pub/Sub pushes.
     *
     * @param channel raw pub/sub channel
     * @return {@code true} when the channel is {@link #SYNC_CHANNEL}
     */
    public boolean isSyncChannel(byte[] channel) {
        return Arrays.equals(SYNC_CHANNEL_BYTES, channel);
    }

    /**
     * Aborts an in-flight {@link #triggerAndAwait(long)}; invoked when replication fails.
     *
     * @param throwable fatal replication error
     */
    private void fail(Throwable throwable) {
        CompletableFuture<Void> future = syncFuture.get();
        if (future != null) {
            future.completeExceptionally(throwable);
        }
    }
}
