package io.github.dimkich.integration.testing.wait.completion.method.pair;

import io.github.dimkich.integration.testing.expression.PointcutId;
import io.github.dimkich.integration.testing.wait.completion.MethodPairAwait;
import net.bytebuddy.asm.Advice;

import java.lang.reflect.Method;

/**
 * ByteBuddy advice responsible for signaling the start of a logical task in a paired method strategy.
 *
 * <p>This advice is injected into methods matched by the {@link MethodPairAwait#startPointcut()}.
 * When the intercepted method is entered, it increments a shared counter in the {@link MethodPairTracker}.
 * This counter is shared with a corresponding "exit" pointcut to track asynchronous tasks that span
 * across different method calls (e.g., a request-response pattern).</p>
 *
 * <h3>Context Capture:</h3>
 * <ul>
 *   <li>{@code @Advice.This}: Captures the target instance for conditional filtering via {@code startWhen}.</li>
 *   <li>{@code @Advice.AllArguments}: Captures arguments for fine-grained task tracking.</li>
 *   <li>{@code @PointcutId}: Used to retrieve the shared counter associated with this pair.</li>
 * </ul>
 *
 * @see MethodPairExitAdvice
 * @see MethodPairTracker
 */
public class MethodPairEnterAdvice {

    /**
     * Invoked immediately upon entering the "start" method of a tracked pair.
     *
     * <p>Reports the task initiation to the {@link MethodPairTracker}. If the runtime
     * condition (if any) is satisfied, the shared counter for this pointcut pair
     * is incremented.</p>
     *
     * @param obj        the instance on which the method is invoked (null for static methods).
     * @param method     the reflected {@link Method} being executed.
     * @param args       array of arguments passed to the method.
     * @param pointcutId unique identifier for the starting point of the pair.
     */
    @Advice.OnMethodEnter
    public static void enter(@Advice.This(optional = true) Object obj,
                             @Advice.Origin Method method,
                             @Advice.AllArguments Object[] args,
                             @PointcutId int pointcutId) {
        MethodPairTracker.startTask(pointcutId, obj, method, args);
    }
}
