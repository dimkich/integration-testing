package io.github.dimkich.integration.testing.wait.completion.future.like;

import io.github.dimkich.integration.testing.expression.PointcutRegistry;
import io.github.dimkich.integration.testing.expression.PointcutSettings;
import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
 * A task is considered active after it is registered via {@link #addTask(int, Object, Object[])}
 * and remains active until {@link #awaitAndCheckActivity()} invokes the await consumer
 * registered for that task and removes it from the registry.
 * <p>
 * The activity flag is set when a task is captured. It is taken atomically after the registry
 * becomes empty, so a task created and completed before the wait is still reported to the
 * coordinator as activity.
 */
public class FutureLikeTracker {
    private static final Logger log = Logger.getLogger(FutureLikeTracker.class.getName());
    private static final Map<Object, Consumer<Object>> activeTasks = new ConcurrentHashMap<>();
    private static final Object lock = new Object();
    @Getter
    private static volatile boolean anyActivity;

    /**
     * Returns the number of currently active tasks.
     *
     * @return count of active tasks
     */
    public static int getActiveTasks() {
        return activeTasks.size();
    }

    /**
     * Clears all currently active tasks and the activity flag.
     */
    public static void reset() {
        synchronized (lock) {
            activeTasks.clear();
            anyActivity = false;
        }
    }

    /**
     * Registers a new future-like object for the upcoming wait. Duplicate registration of
     * the same object is ignored and does not mark activity again.
     *
     * @param pointcutId the unique ID used to fetch the await consumer.
     * @param obj        the captured future-like object.
     * @param args       arguments from the interception point for conditional filtering.
     */
    public static void addTask(int pointcutId, Object obj, Object[] args) {
        PointcutSettings settings = PointcutRegistry.get(pointcutId);
        if (settings.checkWhen(obj, args)) {
            synchronized (lock) {
                if (activeTasks.putIfAbsent(obj, settings.getAwait()) == null) {
                    anyActivity = true;
                    log.log(Level.FINE, "Active : {0}: started: {1}, ",
                            new Object[]{activeTasks.size(), obj});
                }
            }
        }
    }

    /**
     * Waits for completion of all currently active tasks, including tasks registered while
     * the wait is running, and atomically takes the activity accumulated since
     * {@link #reset()} or the previous call.
     * <p>
     * For each active task the await consumer registered for it is invoked and the task is
     * then removed from the registry. This method blocks until there are no more active tasks.
     *
     * @return {@code true} if at least one task was registered during the window
     */
    public static boolean awaitAndCheckActivity() {
        while (!activeTasks.isEmpty()) {
            for (Map.Entry<Object, Consumer<Object>> entry : activeTasks.entrySet()) {
                entry.getValue().accept(entry.getKey());
                activeTasks.remove(entry.getKey(), entry.getValue());
                log.log(Level.FINE, "Active : {0}: ended: {1}, ",
                        new Object[]{activeTasks.size(), entry.getKey()});
            }
        }
        synchronized (lock) {
            boolean activity = anyActivity;
            anyActivity = false;
            log.log(Level.FINE, "WaitCompletion no active FutureLike Objects, activity: {0}",
                    new Object[]{activity});
            return activity;
        }
    }
}
