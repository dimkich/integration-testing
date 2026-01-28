package io.github.dimkich.integration.testing.wait.completion.future.like;

import io.github.dimkich.integration.testing.wait.completion.Pointcut;
import net.bytebuddy.asm.Advice;

/**
 * Byte Buddy advice that intercepts factory methods creating {@code Future}-like instances
 * and registers each created object in {@link FutureLikeTracker} so it can be waited for
 * completion in integration tests.
 */
public class FutureLikeFactoryMethodAdvice {

    /**
     * Called on factory method exit to register the returned {@code Future}-like object.
     *
     * @param obj      the instance returned from the intercepted factory method
     * @param pointcut identifier of the pointcut that matched the method, used for tracking
     */
    @Advice.OnMethodExit
    public static void exit(@Advice.Return Object obj, @Pointcut String pointcut) {
        FutureLikeTracker.addTask(obj, pointcut);
    }
}