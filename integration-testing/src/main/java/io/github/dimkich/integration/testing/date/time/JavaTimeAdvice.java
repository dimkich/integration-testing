package io.github.dimkich.integration.testing.date.time;

import lombok.Setter;

import java.util.TimeZone;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Advice class that provides proxy methods for Java time-related operations.
 * <p>
 * This class is used by ByteBuddy transformations to intercept calls to time-related methods
 * in Java classes. The intercepted methods are:
 * <ul>
 *   <li>{@link System#currentTimeMillis()} - replaced with {@link #currentTimeMillis()}</li>
 *   <li>{@code VM.getNanoTimeAdjustment(long)} - replaced with {@link #getNanoTimeAdjustment(long)}</li>
 *   <li>{@code TimeZone.getDefaultRef()} - replaced with {@link #getDefaultRef()}</li>
 * </ul>
 * </p>
 * <p>
 * Each method checks whether to use the real system implementation or a mocked version
 * based on the configured {@link #callRealMethod} supplier and whether mock implementations
 * have been provided via the setters.
 * </p>
 */
public class JavaTimeAdvice {
    /**
     * Supplier that determines whether real system methods should be called instead of mocked versions.
     * If {@code null} or returns {@code true}, real methods are used.
     */
    @Setter
    private static BooleanSupplier callRealMethod;

    /**
     * Mock implementation supplier for {@link System#currentTimeMillis()}.
     * If {@code null}, the real system method is called.
     */
    @Setter
    private static Supplier<Long> currentTimeMillis;

    /**
     * Real implementation of {@code VM.getNanoTimeAdjustment(long)} accessed via reflection.
     * This is cached to avoid repeated reflection calls.
     */
    @Setter
    private static Function<Long, Long> realGetNanoTimeAdjustment;

    /**
     * Mock implementation function for {@code VM.getNanoTimeAdjustment(long)}.
     * If {@code null}, the real implementation ({@link #realGetNanoTimeAdjustment}) is used.
     */
    @Setter
    private static Function<Long, Long> getNanoTimeAdjustment;

    /**
     * Real implementation of {@code TimeZone.getDefaultRef()} accessed via reflection.
     * This is cached to avoid repeated reflection calls.
     */
    @Setter
    private static Supplier<TimeZone> realGetDefaultRef;

    /**
     * Mock implementation supplier for {@code TimeZone.getDefaultRef()}.
     * If {@code null}, the real implementation ({@link #realGetDefaultRef}) is used.
     */
    @Setter
    private static Supplier<TimeZone> getDefaultRef;

    /**
     * Tracks whether the previous call to {@link #getNanoTimeAdjustment(long)} used the real implementation.
     * Used to detect transitions between real and mocked modes, which triggers a reset signal (return -1).
     */
    private static boolean previousGetNanoTimeAdjustmentIsReal = true;

    /**
     * Proxy method for {@link System#currentTimeMillis()}.
     * <p>
     * Returns either the real system time or a mocked value based on the configuration.
     * The real method is used if:
     * <ul>
     *   <li>{@link #callRealMethod} is {@code null}, or</li>
     *   <li>{@link #currentTimeMillis} is {@code null}, or</li>
     *   <li>{@link #callRealMethod} returns {@code true}</li>
     * </ul>
     * Otherwise, returns the value from the mocked supplier.
     * </p>
     *
     * @return the current time in milliseconds since the epoch, either from the real system
     * or from the mocked implementation
     */
    public static long currentTimeMillis() {
        if (callRealMethod == null || currentTimeMillis == null || callRealMethod.getAsBoolean()) {
            return System.currentTimeMillis();
        }
        return currentTimeMillis.get();
    }

    /**
     * Proxy method for {@code VM.getNanoTimeAdjustment(long)}.
     * <p>
     * This method is used internally by {@link java.time.Clock} to calculate nano time adjustments.
     * It returns either the real adjustment value or a mocked value based on the configuration.
     * </p>
     * <p>
     * When transitioning between real and mocked modes, this method returns {@code -1} as a signal
     * to trigger recalculation in the calling code. The transition is detected by comparing the
     * current mode with {@link #previousGetNanoTimeAdjustmentIsReal}.
     * </p>
     * <p>
     * The real method is used if:
     * <ul>
     *   <li>{@link #callRealMethod} is {@code null}, or</li>
     *   <li>{@link #getNanoTimeAdjustment} is {@code null}, or</li>
     *   <li>{@link #callRealMethod} returns {@code true}</li>
     * </ul>
     * Otherwise, returns the value from the mocked function.
     * </p>
     *
     * @param offsetInSeconds the offset in seconds for which to calculate the nano time adjustment
     * @return the nano time adjustment value, or {@code -1} if transitioning between real and mocked modes
     */
    public static long getNanoTimeAdjustment(long offsetInSeconds) {
        boolean isReal = callRealMethod == null || getNanoTimeAdjustment == null || callRealMethod.getAsBoolean();
        if (isReal != previousGetNanoTimeAdjustmentIsReal) {
            previousGetNanoTimeAdjustmentIsReal = isReal;
            return -1;
        }
        if (isReal) {
            return realGetNanoTimeAdjustment.apply(offsetInSeconds);
        }
        return getNanoTimeAdjustment.apply(offsetInSeconds);
    }

    /**
     * Proxy method for {@code TimeZone.getDefaultRef()}.
     * <p>
     * This method is used internally by {@link TimeZone}, {@link java.util.Date}, and
     * {@link java.util.Calendar} classes to get the default timezone reference.
     * It returns either the real default timezone or a mocked value based on the configuration.
     * </p>
     * <p>
     * The real method is used if:
     * <ul>
     *   <li>{@link #callRealMethod} is {@code null}, or</li>
     *   <li>{@link #getDefaultRef} is {@code null}, or</li>
     *   <li>{@link #callRealMethod} returns {@code true}</li>
     * </ul>
     * Otherwise, returns the value from the mocked supplier.
     * </p>
     *
     * @return the default timezone, either from the real system or from the mocked implementation
     */
    public static TimeZone getDefaultRef() {
        if (callRealMethod == null || getDefaultRef == null || callRealMethod.getAsBoolean()) {
            return realGetDefaultRef.get();
        }
        return getDefaultRef.get();
    }
}
