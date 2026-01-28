package io.github.dimkich.integration.testing.wait.completion.pending.tasks;

import lombok.Getter;
import lombok.Setter;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tracks services that expose a notion of "pending tasks" so that tests can
 * synchronously wait until all background work is finished.
 * <p>
 * Services are registered together with a function that returns the current
 * number of pending tasks. The services are kept via {@link WeakReference}
 * so that normal garbage collection is not inhibited.
 */
public class PendingTasksTracker {
    private static final Logger log = Logger.getLogger(PendingTasksTracker.class.getName());

    /**
     * Functions that know how to obtain the number of pending tasks for a
     * particular pointcut key.
     */
    private static final Map<String, Function<Object, Integer>> countPendingTasksFunctions = new ConcurrentHashMap<>();
    @Setter
    /**
     * Optional filter that decides whether a concrete service instance should
     * be tracked for the given pointcut.
     */
    private static BiPredicate<String, Class<?>> classFilter;

    /**
     * Currently tracked service instances, wrapped in identity-based weak
     * references to avoid memory leaks while still being able to distinguish
     * different instances with identical state.
     */
    private static final Map<IdentityWeakReference, Boolean> activeServices = new ConcurrentHashMap<>();

    /**
     * Reference queue used to clean up entries from {@link #activeServices}
     * when service instances are garbage collected.
     */
    private static final ReferenceQueue<Object> queue = new ReferenceQueue<>();

    /**
     * Clears all registered pointcut functions and tracked services.
     * Intended to be called between tests to reset global state.
     */
    public static void clear() {
        countPendingTasksFunctions.clear();
        activeServices.clear();
    }

    /**
     * Registers a function capable of returning the number of pending tasks
     * for services associated with the given pointcut.
     *
     * @param pointcut a unique key that identifies the selector used for
     *                 instrumentation
     * @param function function that returns the number of pending tasks for
     *                 a service instance
     * @throws IllegalArgumentException if a function for the given pointcut
     *                                  is already registered
     */
    public static void addCountFunction(String pointcut, Function<Object, Integer> function) {
        countPendingTasksFunctions.compute(pointcut, (k, v) -> {
            if (v != null) {
                throw new IllegalArgumentException(String.format("Function with type '%s' already exists", pointcut));
            }
            return function;
        });
    }

    /**
     * Adds a service instance to be tracked for the given pointcut.
     * <p>
     * The service is only tracked if {@link #classFilter} accepts it and if
     * a count function for the pointcut has been registered beforehand.
     *
     * @param service  the service instance whose pending tasks should be tracked
     * @param pointcut the pointcut key that determines which count function
     *                 should be used
     * @throws IllegalArgumentException if the pointcut is unknown
     */
    public static void addService(Object service, String pointcut) {
        if (!classFilter.test(pointcut, service.getClass())) {
            return;
        }
        Function<Object, Integer> count = countPendingTasksFunctions.get(pointcut);
        if (count == null) {
            throw new IllegalArgumentException("Pointcut '" + pointcut + "' not found");
        }
        activeServices.put(new IdentityWeakReference(service, count), Boolean.TRUE);
    }

    /**
     * Returns the total number of pending tasks across all tracked services.
     * <p>
     * Before computing the total, all entries whose referent was garbage
     * collected are removed from {@link #activeServices}.
     *
     * @param allowLogging whether per-service counts should be logged
     * @return total number of pending tasks
     */
    public static int getCount(boolean allowLogging) {
        Object ref;
        while ((ref = queue.poll()) != null) {
            activeServices.remove(ref);
        }
        return activeServices.keySet().stream()
                .mapToInt(r -> r.countPendingTasks(allowLogging))
                .sum();
    }

    /**
     * Weak reference that uses the referent's identity hash code for
     * {@link #hashCode()} and identity equality for {@link #equals(Object)}.
     * <p>
     * This allows distinguishing between different service instances even
     * when they are equal in terms of {@link Object#equals(Object)}.
     */
    private static final class IdentityWeakReference extends WeakReference<Object> {
        private final int identityHashCode;
        @Getter
        private final Function<Object, Integer> countPendingTasksFunction;

        /**
         * Creates a new identity weak reference that is registered with the
         * global {@link #queue} for clean-up.
         *
         * @param referent                  the service instance
         * @param countPendingTasksFunction function that returns the number of
         *                                  pending tasks for the referent
         */
        IdentityWeakReference(Object referent, Function<Object, Integer> countPendingTasksFunction) {
            super(referent, queue);
            this.countPendingTasksFunction = countPendingTasksFunction;
            this.identityHashCode = System.identityHashCode(referent);
        }

        /**
         * Returns the number of pending tasks for the referenced service.
         *
         * @param allowLogging whether the result should be logged
         * @return number of pending tasks, or {@code 0} if the referent has
         * already been garbage collected
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
            if (this.get() != null && that.get() != null) {
                return this.get() == that.get();
            }
            return false;
        }

        @Override
        public int hashCode() {
            return identityHashCode;
        }
    }
}
