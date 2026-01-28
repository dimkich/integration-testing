package io.github.dimkich.integration.testing.wait.completion.method.pair;

import io.github.dimkich.integration.testing.wait.completion.Pointcut;
import net.bytebuddy.asm.Advice;

import java.lang.reflect.Method;

/**
 * Byte Buddy advice that is invoked on method exit to signal the end of a
 * "pair" method execution tracked by {@link MethodPairTracker}.
 * <p>
 * This advice delegates to {@link MethodPairTracker#endTask(Class, Method, String)}
 * to mark the completion of a tracked task associated with the current call.
 */
public class MethodPairExitAdvice {
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    /**
     * Signals that a tracked method has finished execution.
     *
     * @param obj      the instance on which the method was invoked, or {@code null} for static methods
     * @param method   the reflective {@link Method} being exited
     * @param pointcut the pointcut expression identifying which methods are being tracked
     */
    public static void exit(@Advice.This(optional = true) Object obj,
                            @Advice.Origin Method method,
                            @Pointcut String pointcut) {
        MethodPairTracker.endTask(obj == null ? method.getDeclaringClass() : obj.getClass(), method, pointcut);
    }
}
