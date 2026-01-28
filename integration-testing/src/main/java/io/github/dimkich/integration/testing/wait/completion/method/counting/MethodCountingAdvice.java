package io.github.dimkich.integration.testing.wait.completion.method.counting;

import io.github.dimkich.integration.testing.wait.completion.Pointcut;
import net.bytebuddy.asm.Advice;

import java.lang.reflect.Method;

/**
 * ByteBuddy advice that tracks method executions for the
 * method-counting based {@code WaitCompletion} implementation.
 * <p>
 * Every instrumented method call is reported to {@link MethodCountingTracker}
 * on enter and exit so that tests can wait until the configured number of
 * method invocations have completed.
 */
public class MethodCountingAdvice {

    /**
     * Reports the start of the instrumented method execution.
     *
     * @param obj      the instance on which the method is invoked (may be {@code null} for static methods)
     * @param method   the reflective {@link Method} being executed
     * @param pointcut the textual pointcut expression used to select this method
     */
    @Advice.OnMethodEnter
    public static void enter(@Advice.This(optional = true) Object obj, @Advice.Origin Method method,
                             @Pointcut String pointcut) {
        MethodCountingTracker.startTask(obj == null ? method.getDeclaringClass() : obj.getClass(), method, pointcut);
    }

    /**
     * Reports the end of the instrumented method execution.
     * <p>
     * This advice is triggered both on normal completion and when the method
     * exits with an exception.
     *
     * @param obj      the instance on which the method is invoked (may be {@code null} for static methods)
     * @param method   the reflective {@link Method} being executed
     * @param pointcut the textual pointcut expression used to select this method
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(@Advice.This(optional = true) Object obj, @Advice.Origin Method method,
                            @Pointcut String pointcut) {
        MethodCountingTracker.endTask(obj == null ? method.getDeclaringClass() : obj.getClass(), method, pointcut);
    }
}
