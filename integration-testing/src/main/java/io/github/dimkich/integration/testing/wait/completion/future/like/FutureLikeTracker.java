package io.github.dimkich.integration.testing.wait.completion.future.like;

import io.github.dimkich.integration.testing.expression.PointcutRegistry;
import io.github.dimkich.integration.testing.expression.PointcutSettings;
import lombok.Getter;

import java.util.Iterator;
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
 * A task is considered active after it is registered via {@link #addTask(Object, String)}
 * and remains active until {@link #waitCompletion()} invokes the corresponding await
 * consumer for that task and removes it from the registry.
 * </p>
 */
public class FutureLikeTracker {
    private static final Logger log = Logger.getLogger(FutureLikeTracker.class.getName());
    private static final Map<Object, Consumer<Object>> activeTasks = new ConcurrentHashMap<>();
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
     * Clears all currently active tasks.
     */
    public static void reset() {
        activeTasks.clear();
    }

    public static void addTask(int pointcutId, Object obj, Object[] args) {
        PointcutSettings settings = PointcutRegistry.get(pointcutId);
        if (settings.checkWhen(obj, args) && !activeTasks.containsKey(obj)) {
            activeTasks.put(obj, settings.getAwait());
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
