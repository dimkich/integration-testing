package io.github.dimkich.integration.testing.expression;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A thread-safe global registry that manages {@link PointcutSettings} for all instrumented pointcuts.
 *
 * <p>This registry acts as a high-performance lookup table (using a volatile array) that maps
 * integer IDs to their respective behavioral settings. It supports dynamic capacity expansion
 * and provides O(1) access time, which is critical during method interception in the Advice logic.</p>
 */
public class PointcutRegistry {
    /**
     * Initial size of the internal registry array.
     */
    private static final int INITIAL_CAPACITY = 64;

    /**
     * Volatile array holding pointcut settings.
     * Replaced with a larger copy when capacity is exceeded.
     */
    private static volatile PointcutSettings[] registry = new PointcutSettings[INITIAL_CAPACITY];

    /**
     * Generator for unique pointcut identifiers.
     */
    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(0);

    /**
     * Registers a new pointcut, allocates a unique ID, and initializes its settings.
     *
     * @return a unique integer ID associated with the newly created {@link PointcutSettings}.
     */
    public static int register() {
        int id = ID_GENERATOR.getAndIncrement();
        ensureCapacity(id);
        registry[id] = new PointcutSettings();
        return id;
    }

    /**
     * Retrieves the settings for a specific pointcut by its ID.
     *
     * @param id the unique identifier of the pointcut.
     * @return the {@link PointcutSettings} instance associated with the ID.
     * @throws ArrayIndexOutOfBoundsException if the ID is invalid.
     */
    public static PointcutSettings get(int id) {
        return registry[id];
    }

    /**
     * Thread-safe method to ensure the internal array can accommodate the given ID.
     * Uses double-checked locking to perform a synchronized copy-on-write expansion.
     */
    private static void ensureCapacity(int id) {
        if (id >= registry.length) {
            synchronized (PointcutRegistry.class) {
                if (id >= registry.length) {
                    int oldLength = registry.length;
                    int newLength = oldLength << 1;
                    if (id >= newLength) newLength = id + 1;
                    PointcutSettings[] newRegistry = new PointcutSettings[newLength];
                    System.arraycopy(registry, 0, newRegistry, 0, oldLength);
                    registry = newRegistry;
                }
            }
        }
    }

    /**
     * Resets the registry to its initial state and clears all registered settings.
     * <b>Warning:</b> This will invalidate all previously assigned pointcut IDs.
     */
    public static void clear() {
        synchronized (PointcutRegistry.class) {
            registry = new PointcutSettings[INITIAL_CAPACITY];
            ID_GENERATOR.set(0);
        }
    }
}
