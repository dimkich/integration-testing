package io.github.dimkich.integration.testing.wait.completion.method.counting;

import io.github.dimkich.integration.testing.expression.PointcutRegistry;
import lombok.Getter;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A global, thread-safe monitor that tracks the number of currently active method executions
 * and provides synchronization primitives for test execution.
 *
 * <p>This tracker maintains an atomic "in-flight" counter. When the counter reaches zero,
 * it signals any waiting threads that all background activities have completed.
 * It is primarily used by {@link MethodCountingAdvice} to report method lifecycle events.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><b>Atomic Counters:</b> Uses {@link AtomicInteger} to track tasks without heavy locking on every call.</li>
 *   <li><b>Conditional Tracking:</b> Evaluates the 'when' condition of a pointcut before incrementing/decrementing.</li>
 *   <li><b>Wait-Notify Mechanism:</b> Blocks the test thread efficiently until all concurrent tasks finish.</li>
 * </ul>
 *
 * @see MethodCountingAdvice
 * @see MethodCountingWaitCompletion
 */
public class MethodCountingTracker {
    /**
     * Logger used for diagnostic messages about task lifecycle and counter state.
     */
    private static final Logger log = Logger.getLogger(MethodCountingTracker.class.getName());

    /**
     * The number of currently executing (in-flight) tasks.
     */
    private static final AtomicInteger activeTasks = new AtomicInteger(0);

    /**
     * Dedicated monitor object for coordinating {@link #waitCompletion()} and {@link #endTask(int, Object, Method, Object[])}.
     */
    private static final Object lock = new Object();

    /**
     * Flag indicating whether any tracked activity has been observed
     * since the last {@link #reset()} or {@link #waitCompletion()} call.
     */
    @Getter
    private static volatile boolean anyActivity;

    /**
     * Resets the active task counter and the activity flag.
     * <p>Should be called at the start of a test case to ensure no leaked counts
     * from previous tests affect the current results.</p>
     */
    public static void reset() {
        activeTasks.set(0);
        anyActivity = false;
    }

    /**
     * @return the current number of tasks being tracked.
     */
    public static int getActiveTasks() {
        return activeTasks.get();
    }

    /**
     * Increments the active task counter.
     *
     * <p>Called by {@link MethodCountingAdvice#enter}. It retrieves the pointcut
     * settings and evaluates the dynamic 'when' condition. If the condition is met,
     * the counter is incremented and the activity flag is set to {@code true}.</p>
     *
     * @param pointcutId the unique ID used to fetch settings from the {@link PointcutRegistry}.
     * @param obj        the target object of the intercepted method.
     * @param method     the method being executed (for logging).
     * @param args       arguments passed to the method.
     */
    public static void startTask(int pointcutId, Object obj, Method method, Object[] args) {
        if (PointcutRegistry.get(pointcutId).checkWhen(obj, args)) {
            int count = activeTasks.incrementAndGet();
            anyActivity = true;
            log.log(Level.FINE, "Active: {0}: Started: {1}", new Object[]{count, method});
        }
    }

    /**
     * Decrements the active task counter.
     *
     * <p>Called by {@link MethodCountingAdvice#exit}. If the 'when' condition is met,
     * it decrements the counter. If the counter reaches zero, it triggers a
     * {@code notifyAll()} on the internal lock to wake up the waiting test thread.</p>
     *
     * @param pointcutId the unique ID for pointcut settings.
     * @param obj        the target object.
     * @param method     the method that finished execution.
     * @param args       method arguments.
     */
    public static void endTask(int pointcutId, Object obj, Method method, Object[] args) {
        if (PointcutRegistry.get(pointcutId).checkWhen(obj, args)) {
            int count = activeTasks.decrementAndGet();
            log.log(Level.FINE, "Active: {0}: Ended: {1}", new Object[]{count, method});
            if (count == 0) {
                synchronized (lock) {
                    lock.notifyAll();
                }
            }
        }
    }

    /**
     * Blocks the calling thread until there are no active (in-flight) tasks.
     *
     * <p>This method implements a wait loop. It checks the {@link #activeTasks} counter
     * within a {@code synchronized} block. If the counter is greater than zero,
     * it enters a {@code wait(1)} state, waking up either on notification
     * from {@link #endTask} or after a 1ms timeout (to prevent potential deadlocks
     * due to missed notifications or race conditions during counter transitions).</p>
     *
     * @throws InterruptedException if the thread is interrupted while waiting.
     */
    public static void waitCompletion() throws InterruptedException {
        log.log(Level.FINE, "WaitCompletion, active: {0}", new Object[]{activeTasks.get()});
        anyActivity = false;
        if (activeTasks.get() > 0) {
            synchronized (lock) {
                while (activeTasks.get() > 0) {
                    lock.wait(1);
                }
                log.log(Level.FINE, "WaitCompletion, active: {0}", new Object[]{activeTasks.get()});
            }
        }
    }
}
