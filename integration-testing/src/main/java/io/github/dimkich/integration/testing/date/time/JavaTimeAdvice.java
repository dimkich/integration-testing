package io.github.dimkich.integration.testing.date.time;

import lombok.Setter;

import java.util.TimeZone;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

public class JavaTimeAdvice {
    @Setter
    private static BooleanSupplier callRealMethod;
    @Setter
    private static Supplier<Long> currentTimeMillis;
    @Setter
    private static Function<Long, Long> realGetNanoTimeAdjustment;
    @Setter
    private static Function<Long, Long> getNanoTimeAdjustment;
    @Setter
    private static Supplier<TimeZone> realGetDefaultRef;
    @Setter
    private static Supplier<TimeZone> getDefaultRef;

    private static boolean previousGetNanoTimeAdjustmentIsReal = true;

    public static long currentTimeMillis() {
        if (callRealMethod == null || currentTimeMillis == null || callRealMethod.getAsBoolean()) {
            return System.currentTimeMillis();
        }
        return currentTimeMillis.get();
    }

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

    public static TimeZone getDefaultRef() {
        if (callRealMethod == null || getDefaultRef == null || callRealMethod.getAsBoolean()) {
            return realGetDefaultRef.get();
        }
        return getDefaultRef.get();
    }
}
