package io.github.dimkich.integration.testing.wait.completion.method.pair;

import io.github.dimkich.integration.testing.expression.PointcutRegistry;
import lombok.Getter;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A specialized monitor that coordinates asynchronous task tracking through pairs
 * of method invocations (start and end).
 *
 * <p>Unlike standard method counting, this tracker supports scenarios where a task
 * starts in one method (e.g., sending a request) and completes in a different method
 * (e.g., receiving a response). It maps two distinct pointcut IDs to a single
 * {@link AtomicInteger} counter, effectively linking them as a logical pair.</p>
 *
 * <h3>Mechanism:</h3>
 * <ul>
 *   <li><b>Pair Registration:</b> Two pointcut IDs (start and end) are mapped to
 *       the same {@link AtomicInteger} instance via {@link #registerPair(int, int)}.</li>
 *   <li><b>Shared State:</b> Calling {@code startTask} increments the shared counter,
 *       while {@code endTask} decrements the <i>same</i> counter.</li>
 *   <li><b>Global Convergence:</b> The framework blocks until the <b>sum</b> of all
 *       active tasks across all registered shared counters reaches zero.</li>
 * </ul>
 *
 * @see MethodPairEnterAdvice
 * @see MethodPairExitAdvice
 * @see MethodPairWaitCompletion
 */
public class MethodPairTracker {
    private static final Logger log = Logger.getLogger(MethodPairTracker.class.getName());

    /**
     * Map linking pointcut IDs to their respective shared counters.
     * Multiple IDs (start/end) point to the same {@link AtomicInteger} instance.
     */
    private static final Map<Integer, AtomicInteger> counters = new ConcurrentHashMap<>();

    private static final Object lock = new Object();

    /**
     * Flag indicating whether any activity was recorded by any registered
     * method pair since the last reset.
     */
    @Getter
    private static volatile boolean anyActivity;

    /**
     * Links two pointcut IDs into a single logical pair sharing the same atomic counter.
     *
     * @param startId the ID of the pointcut that marks the task beginning (increments counter).
     * @param endId   the ID of the pointcut that marks the task completion (decrements counter).
     */
    public static void registerPair(int startId, int endId) {
        AtomicInteger sharedCounter = new AtomicInteger(0);
        counters.put(startId, sharedCounter);
        counters.put(endId, sharedCounter);
    }

    /**
     * Removes all registered pointcut pairs and their associated counters.
     * <p>This effectively clears the instrumentation rules from the tracker's memory.</p>
     */
    public static void clear() {
        counters.clear();
    }

    /**
     * Resets all shared counters to zero and clears the activity flag.
     * <p>Keeps the registration of pointcut pairs intact, allowing for reuse in
     * subsequent tracking cycles without re-registration.</p>
     */
    public static void reset() {
        counters.values().forEach(c -> c.set(0));
        anyActivity = false;
    }

    /**
     * Calculates the total number of currently active tasks across all
     * unique registered counters.
     *
     * @return the total count of in-flight paired tasks.
     */
    public static int getActiveTasks() {
        return counters.values().stream()
                .distinct()
                .mapToInt(AtomicInteger::get)
                .sum();
    }

    /**
     * Increments the shared counter associated with the given pointcut ID.
     *
     * <p>Invoked by {@link MethodPairEnterAdvice}. If the pointcut's 'when' condition
     * is satisfied, the associated shared counter is incremented and the activity
     * flag is set to {@code true}.</p>
     *
     * @param pointcutId the unique ID used to fetch settings and the shared counter.
     * @param obj        the target object instance.
     * @param method     the method being executed (for logging).
     * @param args       arguments passed to the method.
     */
    public static void startTask(int pointcutId, Object obj, Method method, Object[] args) {
        if (PointcutRegistry.get(pointcutId).checkWhen(obj, args)) {
            AtomicInteger counter = counters.get(pointcutId);
            if (counter != null) {
                counter.incrementAndGet();
                anyActivity = true;
                log.log(Level.FINE, "Active: {0}: Started: {1}", new Object[]{getActiveTasks(), method});
            }
        }
    }

    /**
     * Decrements the shared counter associated with the given pointcut ID.
     *
     * <p>Invoked by {@link MethodPairExitAdvice}. If the total number of active tasks
     * across all pairs drops to zero, it notifies any threads blocked in {@link #waitCompletion()}.</p>
     *
     * @param pointcutId the unique ID for pointcut settings and counter lookup.
     * @param obj        the target object instance.
     * @param method     the method that finished execution.
     * @param args       method arguments.
     */
    public static void endTask(int pointcutId, Object obj, Method method, Object[] args) {
        if (PointcutRegistry.get(pointcutId).checkWhen(obj, args)) {
            AtomicInteger counter = counters.get(pointcutId);
            if (counter != null && counter.get() > 0) {
                counter.decrementAndGet();
                log.log(Level.FINE, "Active: {0}: Ended: {1}", new Object[]{getActiveTasks(), method});
                if (getActiveTasks() == 0) {
                    synchronized (lock) {
                        lock.notifyAll();
                    }
                }
            }
        }
    }

    /**
     * Blocks the calling thread until the total number of active paired tasks reaches zero.
     *
     * <p>Uses a polled wait (1ms) within a synchronized block to ensure visibility
     * of counter updates and prevent potential race conditions.</p>
     *
     * @throws InterruptedException if the thread is interrupted while waiting.
     */
    public static void waitCompletion() throws InterruptedException {
        log.log(Level.FINE, "WaitCompletion, active: {0}", new Object[]{getActiveTasks()});
        anyActivity = false;
        if (getActiveTasks() > 0) {
            synchronized (lock) {
                while (getActiveTasks() > 0) {
                    lock.wait();
                }
                log.log(Level.FINE, "WaitCompletion, active: {0}", new Object[]{getActiveTasks()});
            }
        }
    }
}
