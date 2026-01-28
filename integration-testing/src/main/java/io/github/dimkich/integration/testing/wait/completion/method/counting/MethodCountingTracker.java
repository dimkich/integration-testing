package io.github.dimkich.integration.testing.wait.completion.method.counting;

import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tracks number of in-flight method executions that should be considered as "tasks"
 * for wait-completion logic.
 * <p>
 * The counter is incremented in {@link #startTask(Class, Method, String)} and
 * decremented in {@link #endTask(Class, Method, String)} for methods whose
 * classes are accepted by {@link #classFilter}. The {@link #waitCompletion()}
 * method blocks until all tracked tasks are finished (i.e. the internal counter
 * reaches zero).
 */
public class MethodCountingTracker {
    /**
     * Logger used for fine-grained diagnostic messages about task lifecycle.
     */
    private static final Logger log = Logger.getLogger(MethodCountingTracker.class.getName());

    /**
     * Current number of active (in-flight) tasks.
     */
    private static final AtomicInteger activeTasks = new AtomicInteger(0);

    /**
     * Monitor object used for coordinating {@link #waitCompletion()} with calls
     * to {@link #endTask(Class, Method, String)}.
     */
    private static final Object lock = new Object();

    /**
     * Predicate that decides whether a particular method should be tracked as a task.
     * <p>
     * First argument is a pointcut expression, second argument is the actual target class.
     * It is expected to be configured by the surrounding instrumentation code before any
     * calls to {@link #startTask(Class, Method, String)} or {@link #endTask(Class, Method, String)}.
     */
    @Setter
    private static BiPredicate<String, Class<?>> classFilter;

    /**
     * Flag indicating whether any task was started since the last {@link #reset()}
     * or {@link #waitCompletion()} call.
     */
    @Getter
    private static volatile boolean anyActivity;

    /**
     * Resets internal counters and clears the {@link #anyActivity} flag.
     * <p>
     * Intended to be called before each test or wait-completion cycle.
     */
    public static void reset() {
        activeTasks.set(0);
        anyActivity = false;
    }

    /**
     * Returns current number of active (in-flight) tasks.
     *
     * @return current active task count
     */
    public static int getActiveTasks() {
        return activeTasks.get();
    }

    /**
     * Marks start of a method execution that should be tracked as a task if the
     * {@link #classFilter} accepts the given pointcut and class.
     *
     * @param actualClass actual runtime class where the method is executed
     * @param method      method that has just started
     * @param pointcut    textual pointcut representation used for filtering
     */
    public static void startTask(Class<?> actualClass, Method method, String pointcut) {
        if (!classFilter.test(pointcut, actualClass)) {
            return;
        }
        int count = activeTasks.incrementAndGet();
        anyActivity = true;
        log.log(Level.FINE, "Active: {0}: Started: {1}, ", new Object[]{count, method});
    }

    /**
     * Marks end of a method execution that was previously started via
     * {@link #startTask(Class, Method, String)} if the {@link #classFilter}
     * accepts the given pointcut and class.
     * <p>
     * When the active task count reaches zero all threads waiting in
     * {@link #waitCompletion()} are notified.
     *
     * @param actualClass actual runtime class where the method is executed
     * @param method      method that has just finished
     * @param pointcut    textual pointcut representation used for filtering
     */
    public static void endTask(Class<?> actualClass, Method method, String pointcut) {
        if (!classFilter.test(pointcut, actualClass)) {
            return;
        }
        int count = activeTasks.decrementAndGet();
        log.log(Level.FINE, "Active: {0}: Ended: {1}}", new Object[]{count, method});
        if (count == 0) {
            synchronized (lock) {
                lock.notifyAll();
            }
        }
    }

    /**
     * Waits until all tracked tasks are completed.
     * <p>
     * It returns as soon as the active task counter reaches zero.
     *
     * @throws InterruptedException if the current thread is interrupted while waiting
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
