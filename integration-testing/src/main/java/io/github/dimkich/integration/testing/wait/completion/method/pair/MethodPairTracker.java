package io.github.dimkich.integration.testing.wait.completion.method.pair;

import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tracks pairs of method invocations that represent the start and end of logical tasks.
 * <p>
 * Each registered pair of pointcuts shares a single counter. When a start pointcut is hit,
 * the counter is incremented; when the corresponding end pointcut is hit, it is decremented.
 * The tracker can then be used to wait until all tracked tasks are completed.
 */
public class MethodPairTracker {
    private static final Logger log = Logger.getLogger(MethodPairTracker.class.getName());
    private static final Map<String, AtomicInteger> start = new ConcurrentHashMap<>();
    private static final Map<String, AtomicInteger> end = new ConcurrentHashMap<>();

    private static final Object lock = new Object();
    @Setter
    private static BiPredicate<String, Class<?>> classFilter;
    @Getter
    private static volatile boolean anyActivity;

    /**
     * Registers a new pair of pointcuts that will share a single activity counter.
     *
     * @param startPointcut the identifier of the start pointcut
     * @param endPointcut   the identifier of the end pointcut
     * @throws IllegalArgumentException if either pointcut was already registered
     */
    public static void register(String startPointcut, String endPointcut) {
        AtomicInteger integer = new AtomicInteger();
        if (start.containsKey(startPointcut)) {
            throw new IllegalArgumentException("startPointcut '" + startPointcut + "' already exists");
        }
        if (end.containsKey(endPointcut)) {
            throw new IllegalArgumentException("endPointcut '" + endPointcut + "' already exists");
        }
        start.put(startPointcut, integer);
        end.put(endPointcut, integer);
    }

    /**
     * Removes all registered pointcuts and their counters.
     * <p>
     * This method does not change {@link #anyActivity}; use {@link #reset()} when
     * you need to clear both counters and activity flag between tests.
     */
    public static void clear() {
        start.clear();
        end.clear();
    }

    /**
     * Resets all counters to zero and clears the {@link #anyActivity} flag
     * while keeping the set of registered pointcuts.
     */
    public static void reset() {
        for (AtomicInteger integer : start.values()) {
            integer.set(0);
        }
        anyActivity = false;
    }

    /**
     * Returns the total number of currently active tasks across all registered pointcuts.
     *
     * @return the sum of all counters
     */
    public static int getActiveTasks() {
        return start.values().stream().mapToInt(AtomicInteger::get).sum();
    }

    /**
     * Marks the beginning of a task for the given pointcut and method.
     * <p>
     * The start is only recorded if {@link #classFilter} accepts the given class
     * and pointcut; otherwise the call is ignored.
     *
     * @param actualClass the runtime class where the method is executed
     * @param method      the method representing the start of the task
     * @param pointcut    the pointcut identifier that was registered via {@link #register(String, String)}
     */
    public static void startTask(Class<?> actualClass, Method method, String pointcut) {
        if (!classFilter.test(pointcut, actualClass)) {
            return;
        }
        start.get(pointcut).incrementAndGet();
        anyActivity = true;
        log.log(Level.FINE, "Active: {0}: Started: {1}, ", new Object[]{getActiveTasks(), method});
    }

    /**
     * Marks the end of a task for the given pointcut and method.
     * <p>
     * If the counter for this pointcut is greater than zero it is decremented.
     * When the counter reaches zero, all threads waiting in {@link #waitCompletion()}
     * are notified.
     *
     * @param actualClass the runtime class where the method is executed
     * @param method      the method representing the end of the task
     * @param pointcut    the pointcut identifier that was registered via {@link #register(String, String)}
     */
    public static void endTask(Class<?> actualClass, Method method, String pointcut) {
        if (!classFilter.test(pointcut, actualClass)) {
            return;
        }
        AtomicInteger integer = end.get(pointcut);
        int count = 0;
        if (integer.get() > 0) {
            count = end.get(pointcut).decrementAndGet();
            log.log(Level.FINE, "Active: {0}: Ended: {1}}", new Object[]{getActiveTasks(), method});
        }
        if (count == 0) {
            synchronized (lock) {
                lock.notifyAll();
            }
        }
    }

    /**
     * Blocks the current thread until there are no active tasks.
     * <p>
     * If there is no activity at the moment of invocation, this method returns immediately.
     *
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public static void waitCompletion() throws InterruptedException {
        log.log(Level.FINE, "WaitCompletion, active: {0}", new Object[]{getActiveTasks()});
        anyActivity = false;
        if (getActiveTasks() > 0) {
            synchronized (lock) {
                while (getActiveTasks() > 0) {
                    lock.wait(1);
                }
                log.log(Level.FINE, "WaitCompletion, active: {0}", new Object[]{getActiveTasks()});
            }
        }
    }
}
