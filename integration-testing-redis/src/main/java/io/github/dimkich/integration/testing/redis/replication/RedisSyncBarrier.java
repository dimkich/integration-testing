package io.github.dimkich.integration.testing.redis.replication;

import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Pub/sub handshake that blocks test code until a synthetic {@code PUBLISH} has been replicated
 * into the in-memory store.
 * <p>
 * {@link #triggerAndAwait(long)} publishes a unique token on {@link #SYNC_CHANNEL} via the live
 * Redis connection. The replicator eventually emits a matching {@code PublishCommand}; stream
 * handlers forward it to {@link #release(String, String)}, which completes the wait. That proves
 * the replication stream has caught up to at least the barrier command—used by
 * {@link RedisInMemoryStore#getSnapshot()} before returning assertion data.
 * <p>
 * The barrier is only effective while {@link #activate()} has been called (typically during live
 * command sync in {@link io.github.dimkich.integration.testing.redis.replication.event.listener.RedisSyncStateDelegator}).
 * During RDB snapshot sync or unknown phases, {@link #deactivate()} makes
 * {@link #triggerAndAwait(long)} a no-op. Fatal errors from replicator event processing are
 * propagated via {@link #setFatalError(Throwable)}.
 *
 * @see io.github.dimkich.integration.testing.redis.replication.handler.stream.PublishCommandHandler
 */
@Slf4j
@RequiredArgsConstructor
public class RedisSyncBarrier {
    /** Redis channel used for barrier tokens; must not collide with application pub/sub. */
    public static final String SYNC_CHANNEL = "__integration_testing_sync__";

    private final String name;
    private final RedisConnectionFactory connectionFactory;
    private final AtomicReference<String> expectedToken = new AtomicReference<>();
    private final AtomicReference<CompletableFuture<Void>> syncFuture = new AtomicReference<>();
    private final AtomicLong tokenSequence = new AtomicLong(0);
    private final Object activationMonitor = new Object();
    private volatile boolean active;
    private volatile boolean activatedOnce;
    private volatile Throwable fatalError;

    /** Enables barrier waits; invoked when live command replication starts. */
    public void activate() {
        synchronized (activationMonitor) {
            this.active = true;
            this.activatedOnce = true;
            activationMonitor.notifyAll();
        }
    }

    /** Disables barrier waits; {@link #triggerAndAwait(long)} returns immediately without publishing. */
    public void deactivate() {
        this.active = false;
    }

    /**
     * Waits for live command sync (when it is expected) and then for the replication stream
     * to catch up to a barrier token.
     * <p>
     * If the barrier has never been activated — for example, a manually driven test replicator
     * without a real connection — activation is not awaited and this method behaves exactly like
     * {@link #triggerAndAwait(long)}.
     *
     * @param timeoutMs maximum wait in milliseconds for activation and for the barrier token
     * @throws RuntimeException if command sync does not resume or the token is not replicated
     */
    public void awaitSynchronized(long timeoutMs) {
        if (activatedOnce) {
            awaitActivation(timeoutMs);
        }
        triggerAndAwait(timeoutMs);
    }

    @SneakyThrows
    private void awaitActivation(long timeoutMs) {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        synchronized (activationMonitor) {
            while (!active) {
                Throwable error = fatalError;
                if (error != null) {
                    fatalError = null;
                    throw error;
                }
                long remainingNanos = deadline - System.nanoTime();
                if (remainingNanos <= 0) {
                    throw new RuntimeException("Redis Sync Timeout for [" + name + "]: command sync did not resume");
                }
                activationMonitor.wait(TimeUnit.NANOSECONDS.toMillis(remainingNanos) + 1);
            }
        }
    }

    /**
     * Records a fatal replicator error and fails any in-flight {@link #triggerAndAwait(long)}.
     *
     * @param throwable error from event processing; rethrown on the next barrier trigger
     */
    public void setFatalError(Throwable throwable) {
        this.fatalError = throwable;
        CompletableFuture<Void> future = syncFuture.get();
        if (future != null) {
            future.completeExceptionally(throwable);
        }
    }

    /**
     * Publishes a barrier token and waits until replication delivers the same token back, or times out.
     * <p>
     * Does nothing when inactive. Rethrows a previously recorded fatal error from
     * {@link #setFatalError(Throwable)}.
     *
     * @param timeoutMs maximum wait in milliseconds
     * @throws RuntimeException if the wait times out or is interrupted
     */
    @SneakyThrows
    public void triggerAndAwait(long timeoutMs) {
        Throwable error = fatalError;
        if (error != null) {
            fatalError = null;
            throw error;
        }
        if (!active) {
            return;
        }
        CompletableFuture<Void> future = new CompletableFuture<>();
        syncFuture.set(future);

        String token = name + ":" + tokenSequence.incrementAndGet();
        expectedToken.set(token);

        @Cleanup RedisConnection connection = connectionFactory.getConnection();
        connection.publish(SYNC_CHANNEL.getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8));
        log.debug("Sync barrier (PUBLISH) triggered for {} with token {}", name, token);
        try {
            future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
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
    public void release(String channel, String message) {
        if (SYNC_CHANNEL.equals(channel) && message.equals(expectedToken.get())) {
            CompletableFuture<Void> future = syncFuture.get();
            if (future != null) {
                future.complete(null);
                log.debug("Sync barrier released for {} via PUBLISH token", name);
            }
        }
    }
}