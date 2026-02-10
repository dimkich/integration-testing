package io.github.dimkich.integration.testing.wait.completion.method.pair;

import io.github.dimkich.integration.testing.expression.PointcutId;
import io.github.dimkich.integration.testing.wait.completion.MethodPairAwait;
import net.bytebuddy.asm.Advice;

import java.lang.reflect.Method;

/**
 * ByteBuddy advice responsible for signaling the completion of a logical task in a paired method strategy.
 *
 * <p>This advice is injected into methods matched by the {@link MethodPairAwait#endPointcut()}.
 * Unlike standard method counting, this advice decrements a counter that was previously
 * incremented by a <i>different</i> method (the "start" method). This allows the framework
 * to wait for complex asynchronous flows where the task lifecycle is managed by multiple components.</p>
 *
 * <h3>Safety:</h3>
 * <ul>
 *   <li>Uses {@code onThrowable = Throwable.class} to ensure that the task is marked as finished
 *       even if the completion-signaling method fails with an exception.</li>
 *   <li>Delegates to {@link MethodPairTracker#endTask}, which handles the shared counter decrement.</li>
 * </ul>
 *
 * @see MethodPairEnterAdvice
 * @see MethodPairTracker
 */
public class MethodPairExitAdvice {

    /**
     * Invoked when the "end" method of a tracked pair finishes execution.
     *
     * <p>Signals the completion of a previously started task to the {@link MethodPairTracker}.
     * If the shared counter reaches zero across all registered pairs, any threads waiting
     * for completion will be notified.</p>
     *
     * @param obj        the instance on which the method was invoked.
     * @param method     the reflected {@link Method} that finished execution.
     * @param args       array of arguments passed to the method.
     * @param pointcutId unique identifier for the ending point of the pair.
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(@Advice.This(optional = true) Object obj,
                            @Advice.Origin Method method,
                            @Advice.AllArguments Object[] args,
                            @PointcutId int pointcutId) {
        MethodPairTracker.endTask(pointcutId, obj, method, args);
    }
}
