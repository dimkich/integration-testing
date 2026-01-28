package io.github.dimkich.integration.testing.wait.completion.pending.tasks;

import io.github.dimkich.integration.testing.wait.completion.Pointcut;
import net.bytebuddy.asm.Advice;

/**
 * Byte Buddy {@link Advice} that tracks objects created by factory methods as pending-task services.
 * <p>
 * On successful factory method completion the returned object is registered in {@link PendingTasksTracker}.
 */
public class PendingTasksFactoryMethodAdvice {

    /**
     * Registers the object returned from a factory method in {@link PendingTasksTracker} if it is not {@code null}.
     *
     * @param obj      the object returned by the intercepted factory method
     * @param pointcut the textual representation of the selector that identified the intercepted method
     */
    @Advice.OnMethodExit
    public static void exit(@Advice.Return Object obj, @Pointcut String pointcut) {
        if (obj != null) {
            PendingTasksTracker.addService(obj, pointcut);
        }
    }
}