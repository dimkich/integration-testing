package io.github.dimkich.integration.testing.wait.completion.future.like;

import io.github.dimkich.integration.testing.wait.completion.Pointcut;
import net.bytebuddy.asm.Advice;

/**
 * Byte Buddy advice that is applied to constructors of {@code Future}-like objects.
 * <p>
 * On constructor exit it registers the newly created instance in {@link FutureLikeTracker}
 * so that the wait-completion mechanism can track its lifecycle.
 */
public class FutureLikeConstructorAdvice {

    /**
     * Advice method executed on constructor exit.
     *
     * @param obj      the constructed object instance intercepted by Byte Buddy
     * @param pointcut textual representation of the selector that identified this constructor
     */
    @Advice.OnMethodExit
    public static void exit(@Advice.This Object obj, @Pointcut String pointcut) {
        FutureLikeTracker.addTask(obj, pointcut);
    }
}