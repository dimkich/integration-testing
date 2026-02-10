package io.github.dimkich.integration.testing.wait.completion.queue.like;

import io.github.dimkich.integration.testing.expression.PointcutRegistry;
import io.github.dimkich.integration.testing.expression.PointcutSettings;
import lombok.Getter;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A specialized tracker that monitors services exposing a "pending tasks" count.
 *
 * <p>Unlike event-based trackers, this component uses a <b>polling strategy</b>.
 * It maintains a registry of active service instances and periodically queries
 * their state (e.g., queue size, pool occupancy) to determine if background
 * activity is still ongoing.</p>
 *
 * <h3>Memory Management:</h3>
 * <ul>
 *   <li><b>Weak References:</b> Services are stored using {@link WeakReference}
 *       to ensure that the tracker does not prevent the garbage collection
 *       of services no longer used by the application.</li>
 *   <li><b>Reference Queue:</b> A {@link ReferenceQueue} is used to efficiently
 *       remove stale entries from the internal map as soon as their referents
 *       are collected.</li>
 *   <li><b>Identity Semantics:</b> Uses {@code identityHashCode} to distinguish
 *       between instances even if they override {@code equals()} or {@code hashCode()}.</li>
 * </ul>
 *
 * @see QueueLikeAdvice
 * @see QueueLikeWaitCompletion
 */
public class QueueLikeTracker {
    private static final Logger log = Logger.getLogger(QueueLikeTracker.class.getName());

    /**
     * Map of currently tracked services. The key is an identity-based weak reference
     * that stores the polling function.
     */
    private static final Map<IdentityWeakReference, Boolean> activeServices = new ConcurrentHashMap<>();

    /**
     * Queue for cleaning up weak references whose target objects have been garbage collected.
     */
    private static final ReferenceQueue<Object> queue = new ReferenceQueue<>();

    /**
     * Clears all registered services.
     * Typically called between tests to ensure a clean state.
     */
    public static void clear() {
        activeServices.clear();
    }

    /**
     * Registers a new service instance for polling.
     *
     * @param pointcutId the unique ID to retrieve settings (including the 'count' function).
     * @param service    the service instance to monitor.
     * @param args       arguments from the interception point for conditional filtering.
     */
    public static void addService(int pointcutId, Object service, Object[] args) {
        PointcutSettings settings = PointcutRegistry.get(pointcutId);
        if (settings.checkWhen(service, args)) {
            // IdentityWeakReference ensures we track unique instances correctly
            activeServices.put(new IdentityWeakReference(service, settings.getCount()), Boolean.TRUE);
        }
    }

    /**
     * Calculates the total number of pending tasks across all tracked services.
     *
     * <p>Before calculation, it drains the {@link ReferenceQueue} to remove
     * references to objects that have been garbage collected.</p>
     *
     * @param allowLogging if {@code true}, logs the task count for each individual service.
     * @return the sum of all pending tasks reported by the registered count functions.
     */
    public static int getCount(boolean allowLogging) {
        Object ref;
        // Clean up collected references
        while ((ref = queue.poll()) != null) {
            activeServices.remove(ref);
        }

        return activeServices.keySet().stream()
                .mapToInt(r -> r.countPendingTasks(allowLogging))
                .sum();
    }

    /**
     * A specialized {@link WeakReference} that uses {@link System#identityHashCode(Object)}
     * for its own hash code and identity comparison for equality.
     *
     * <p>This allows the tracker to distinguish between two different instances
     * of the same service class even if they are considered "equal" by
     * their domain logic.</p>
     */
    private static final class IdentityWeakReference extends WeakReference<Object> {
        private final int identityHashCode;

        @Getter
        private final Function<Object, Integer> countPendingTasksFunction;

        /**
         * Creates a new identity-based weak reference.
         *
         * @param referent                  the service to track.
         * @param countPendingTasksFunction the function used to poll the task count
         *                                  from this specific referent.
         */
        IdentityWeakReference(Object referent, Function<Object, Integer> countPendingTasksFunction) {
            super(referent, queue);
            this.countPendingTasksFunction = countPendingTasksFunction;
            this.identityHashCode = System.identityHashCode(referent);
        }

        /**
         * Invokes the registered count function on the referent.
         *
         * @param allowLogging whether to log diagnostic info.
         * @return current task count or {@code 0} if the referent has been collected.
         */
        public int countPendingTasks(boolean allowLogging) {
            Object referent = get();
            if (referent == null) {
                return 0;
            }
            int count = countPendingTasksFunction.apply(referent);
            if (allowLogging) {
                log.log(Level.FINE, "Active: {0}, name: {1}, ", new Object[]{count, referent});
            }
            return count;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof IdentityWeakReference that)) {
                return false;
            }

            Object thisRef = this.get();
            Object thatRef = that.get();

            // Strictly check for reference equality
            if (thisRef != null && thatRef != null) {
                return thisRef == thatRef;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return identityHashCode;
        }
    }
}
