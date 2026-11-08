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
 * it signals any waiting threads that all background activities have completed. The counter
 * and the activity flag are mutated under the same monitor, so a task that registers while
 * the test thread reads and resets the flag is either awaited or reported as activity; the
 * event can never be lost between the quiescence check and the reset. It is primarily used
 * by {@link MethodCountingAdvice} to report method lifecycle events.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><b>Atomic Counters:</b> Uses {@link AtomicInteger} to track tasks without heavy locking
 *       on every call; the monitor is held only for a short increment/decrement.</li>
 *   <li><b>Conditional Tracking:</b> Evaluates the 'when' condition of a pointcut before
 *       incrementing/decrementing.</li>
 *   <li><b>Activity Window:</b> The flag is set on task start and taken atomically after the
 *       counter reaches zero, so a cascade that finished before the wait is still reported.</li>
 *   <li><b>Wait-Notify Mechanism:</b> Blocks the test thread efficiently until all concurrent
 *       tasks finish.</li>
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
     * Dedicated monitor that guards counter transitions and the activity flag.
     */
    private static final Object lock = new Object();

    /**
     * Flag indicating whether any tracked activity has been observed since the last
     * {@link #reset()} or {@link #awaitAndCheckActivity()} call.
     */
    @Getter
    private static volatile boolean anyActivity;

    /**
     * Resets the active task counter and the activity flag.
     * <p>Should be called at the start of a test case to ensure no leaked counts
     * from previous tests affect the current results.</p>
     */
    public static void reset() {
        synchronized (lock) {
            activeTasks.set(0);
            anyActivity = false;
        }
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
     * the counter is incremented and the activity flag is set to {@code true} under
     * the monitor, so a concurrent {@link #awaitAndCheckActivity()} cannot miss it.</p>
     *
     * @param pointcutId the unique ID used to fetch settings from the {@link PointcutRegistry}.
     * @param obj        the target object of the intercepted method.
     * @param method     the method being executed (for logging).
     * @param args       arguments passed to the method.
     */
    public static void startTask(int pointcutId, Object obj, Method method, Object[] args) {
        if (PointcutRegistry.get(pointcutId).checkWhen(obj, args)) {
            int count;
            synchronized (lock) {
                count = activeTasks.incrementAndGet();
                anyActivity = true;
            }
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
            int count;
            synchronized (lock) {
                count = activeTasks.decrementAndGet();
                if (count == 0) {
                    lock.notifyAll();
                }
            }
            log.log(Level.FINE, "Active: {0}: Ended: {1}", new Object[]{count, method});
        }
    }

    /**
     * Blocks the calling thread until there are no active (in-flight) tasks, then atomically
     * takes the activity accumulated since {@link #reset()} or the previous call.
     *
     * <p>The wait and the flag reset are performed under the same monitor that guards task
     * registration, so no event can slip between the quiescence check and the reset.</p>
     *
     * @return {@code true} if at least one tracked method was invoked during the window
     * @throws InterruptedException if the thread is interrupted while waiting.
     */
    public static boolean awaitAndCheckActivity() throws InterruptedException {
        synchronized (lock) {
            while (activeTasks.get() > 0) {
                lock.wait();
            }
            boolean activity = anyActivity;
            anyActivity = false;
            log.log(Level.FINE, "WaitCompletion, active: {0}, activity: {1}",
                    new Object[]{activeTasks.get(), activity});
            return activity;
        }
    }
}
