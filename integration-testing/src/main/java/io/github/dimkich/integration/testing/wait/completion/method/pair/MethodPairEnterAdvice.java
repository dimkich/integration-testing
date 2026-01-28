package io.github.dimkich.integration.testing.wait.completion.method.pair;

import io.github.dimkich.integration.testing.wait.completion.Pointcut;
import net.bytebuddy.asm.Advice;

import java.lang.reflect.Method;

/**
 * ByteBuddy advice that marks the start of a paired method invocation.
 * <p>
 * Used together with {@link MethodPairExitAdvice} to track method entry/exit pairs
 * via {@link MethodPairTracker} so that asynchronous tasks can be awaited in tests.
 */
public class MethodPairEnterAdvice {
    @Advice.OnMethodEnter
    public static void enter(@Advice.This(optional = true) Object obj, @Advice.Origin Method method,
                             @Pointcut String pointcut) {
        MethodPairTracker.startTask(obj == null ? method.getDeclaringClass() : obj.getClass(), method, pointcut);
    }
}
