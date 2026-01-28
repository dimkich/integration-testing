package io.github.dimkich.integration.testing.wait.completion.future.like;

import lombok.Getter;
import lombok.Setter;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tracks "future-like" asynchronous tasks that should be awaited during test execution.
 * <p>
 * The tracker maintains:
 * <ul>
 *     <li>a registry of pointcut-specific await consumers, and</li>
 *     <li>a set of currently active tasks.</li>
 * </ul>
 * A task is considered active after it is registered via {@link #addTask(Object, String)}
 * and remains active until {@link #waitCompletion()} invokes the corresponding await
 * consumer for that task and removes it from the registry.
 * </p>
 */
public class FutureLikeTracker {
    private static final Logger log = Logger.getLogger(FutureLikeTracker.class.getName());
    private static final Map<String, Consumer<Object>> awaitConsumers = new ConcurrentHashMap<>();
    /**
     * Predicate used to decide whether a given task should be tracked for a particular pointcut.
     * <p>
     * This predicate must be configured before any calls to {@link #addTask(Object, String)};
     * otherwise a {@link NullPointerException} will occur when it is evaluated.
     * </p>
     */
    @Setter
    private static BiPredicate<String, Class<?>> classFilter;
    private static final Map<Object, Consumer<Object>> activeTasks = new ConcurrentHashMap<>();
    @Getter
    private static volatile boolean anyActivity;

    /**
     * Registers an await consumer for the given pointcut.
     *
     * @param pointcut      identifier of the pointcut used to match tracked tasks
     * @param awaitConsumer consumer that performs the actual waiting logic for a task;
     *                      must not already be registered for the given pointcut
     * @throws IllegalArgumentException if an await consumer is already registered for the given pointcut
     */
    public static void addAwaitConsumer(String pointcut, Consumer<Object> awaitConsumer) {
        awaitConsumers.compute(pointcut, (k, v) -> {
            if (v != null) {
                throw new IllegalArgumentException(String.format("Consumer with type '%s' already exists", pointcut));
            }
            return awaitConsumer;
        });
    }

    /**
     * Clears all registered await consumers.
     * <p>
     * This does not affect already registered active tasks, but it removes the
     * mapping for future registrations.
     * </p>
     */
    public static void clearAwaitConsumers() {
        awaitConsumers.clear();
    }

    /**
     * Returns the number of currently active tasks.
     *
     * @return count of active tasks
     */
    public static int getActiveTasks() {
        return activeTasks.size();
    }

    /**
     * Clears all currently active tasks.
     */
    public static void reset() {
        activeTasks.clear();
    }

    /**
     * Registers a new active task for the given pointcut.
     * <p>
     * The task is added only if it is not already tracked and the configured
     * {@link #classFilter} accepts the provided pointcut and task type. It is expected
     * that an await consumer has been registered for the same pointcut via
     * {@link #addAwaitConsumer(String, Consumer)}; otherwise {@link #waitCompletion()}
     * may fail with a {@link NullPointerException} when attempting to invoke it.
     * </p>
     *
     * @param obj      task instance to track
     * @param pointcut pointcut identifier used to resolve the corresponding await consumer
     */
    public static void addTask(Object obj, String pointcut) {
        if (!activeTasks.containsKey(obj) && classFilter.test(pointcut, obj.getClass())) {
            activeTasks.put(obj, awaitConsumers.get(pointcut));
            anyActivity = true;
            log.log(Level.FINE, "Active : {0}: started: {1}, ", new Object[]{activeTasks.size(), obj});
        }
    }

    /**
     * Waits for completion of all currently active tasks.
     * <p>
     * For each active task the corresponding await consumer is invoked and the task
     * is then removed from the registry. This method blocks until there are no
     * more active tasks.
     * </p>
     */
    public static void waitCompletion() throws InterruptedException {
        anyActivity = false;
        while (!activeTasks.isEmpty()) {
            Thread.sleep(1);
            Iterator<Map.Entry<Object, Consumer<Object>>> iterator = activeTasks.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Object, Consumer<Object>> entry = iterator.next();
                entry.getValue().accept(entry.getKey());
                iterator.remove();
                log.log(Level.FINE, "Active : {0}: ended: {1}, ",
                        new Object[]{activeTasks.size(), entry.getKey()});
            }
        }
        log.log(Level.FINE, "WaitCompletion no active FutureLike Objects");
    }
}
