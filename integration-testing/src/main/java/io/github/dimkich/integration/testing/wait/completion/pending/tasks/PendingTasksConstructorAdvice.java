package io.github.dimkich.integration.testing.wait.completion.pending.tasks;

import io.github.dimkich.integration.testing.wait.completion.Pointcut;
import net.bytebuddy.asm.Advice;

/**
 * Byte Buddy constructor advice that registers newly created service instances
 * in {@link PendingTasksTracker} so that they can be observed by the
 * {@link io.github.dimkich.integration.testing.wait.completion.pending.tasks.PendingTasksWaitCompletion}
 * wait completion strategy.
 *
 * <p>The advice is applied to constructors matched by the pending-tasks pointcut
 * configuration. On every successful constructor exit, the created object is
 * passed to {@link PendingTasksTracker#addService(Object, String)} together with
 * the resolved pointcut expression.</p>
 */
public class PendingTasksConstructorAdvice {

    /**
     * Registers the constructed object in {@link PendingTasksTracker} on constructor exit.
     *
     * @param obj      the constructed instance, injected by Byte Buddy via {@link Advice.This}
     * @param pointcut the pointcut expression associated with this constructor, injected via {@link Pointcut}
     */
    @Advice.OnMethodExit
    public static void exit(@Advice.This Object obj, @Pointcut String pointcut) {
        if (obj != null) {
            PendingTasksTracker.addService(obj, pointcut);
        }
    }
}
