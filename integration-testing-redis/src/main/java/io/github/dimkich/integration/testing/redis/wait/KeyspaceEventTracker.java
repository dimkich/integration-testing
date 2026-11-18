package io.github.dimkich.integration.testing.redis.wait;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cumulative counter of invocations of instrumented keyspace notification handlers, together
 * with the pointcut ids assigned during instrumentation.
 * <p>
 * One counter is kept per pointcut id. Pointcut ids are global and generated when the agent is
 * installed; the notification handler looks its id up by the pointcut expression from the
 * annotation. The class uses only JDK collections, so it is safe in any class loader.
 */
public final class KeyspaceEventTracker {
    private static final Map<String, Integer> pointcutIds = new ConcurrentHashMap<>();
    private static final Map<Integer, Long> calls = new ConcurrentHashMap<>();
    private static final Object lock = new Object();

    private KeyspaceEventTracker() {
    }

    /** Stores the pointcut id assigned by the instrumentation for the given expression. */
    public static void registerPointcut(String pointcut, int pointcutId) {
        pointcutIds.put(pointcut, pointcutId);
    }

    /**
     * Returns the pointcut id assigned during instrumentation.
     *
     * @throws IllegalStateException if the expression was not instrumented in this test class
     */
    public static int pointcutId(String pointcut) {
        Integer id = pointcutIds.get(pointcut);
        if (id == null) {
            throw new IllegalStateException("Keyspace pointcut is not instrumented: " + pointcut);
        }
        return id;
    }

    /** Records one completed handler invocation. */
    public static void onCall(int pointcutId) {
        synchronized (lock) {
            calls.merge(pointcutId, 1L, Long::sum);
            lock.notifyAll();
        }
    }

    /** Returns the cumulative number of handler invocations for the pointcut. */
    public static long calls(int pointcutId) {
        return calls.getOrDefault(pointcutId, 0L);
    }

    /**
     * Blocks until the handler has been called at least {@code expected} times.
     *
     * @param pointcutId pointcut id of the handler
     * @param expected   required cumulative number of calls
     * @param timeoutMs  maximum wait in milliseconds
     * @throws IllegalStateException if the calls do not arrive in time
     */
    public static void awaitCallsAtLeast(int pointcutId, long expected, long timeoutMs) {
        long deadline = System.nanoTime() + timeoutMs * 1_000_000L;
        synchronized (lock) {
            while (calls.getOrDefault(pointcutId, 0L) < expected) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) {
                    throw new IllegalStateException("Timed out waiting for keyspace handler calls: pointcut "
                            + pointcutId + ", expected at least " + expected + ", actual "
                            + calls.getOrDefault(pointcutId, 0L));
                }
                try {
                    lock.wait(Math.max(1L, remaining / 1_000_000L));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for keyspace handler calls", e);
                }
            }
        }
    }

    /** Clears all counters and pointcut ids, used when the instrumentation is torn down. */
    public static void reset() {
        synchronized (lock) {
            calls.clear();
            pointcutIds.clear();
        }
    }
}
