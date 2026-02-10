package io.github.dimkich.integration.testing.wait.completion.method.counting;

import io.github.dimkich.integration.testing.expression.PointcutId;
import net.bytebuddy.asm.Advice;

import java.lang.reflect.Method;

/**
 * ByteBuddy advice responsible for intercepting method entry and exit to maintain
 * the active task counter.
 *
 * <p>This advice is injected into methods matched by {@link io.github.dimkich.integration.testing.wait.completion.MethodCountingAwait}
 * pointcuts. It acts as a bridge between the instrumented application code and the
 * {@link MethodCountingTracker}, ensuring that every started "task" is eventually
 * reported as finished.</p>
 *
 * <h3>Interception Points:</h3>
 * <ul>
 *   <li><b>OnMethodEnter:</b> Increments the global counter if the 'when' condition is met.</li>
 *   <li><b>OnMethodExit:</b> Decrements the global counter. This is configured to catch
 *       all {@link Throwable} types to prevent counter leaks in case of runtime exceptions.</li>
 * </ul>
 *
 * @see MethodCountingTracker
 * @see io.github.dimkich.integration.testing.expression.PointcutId
 */
public class MethodCountingAdvice {

    /**
     * Invoked immediately upon entering the instrumented method.
     *
     * <p>This method reports a new task start to the {@link MethodCountingTracker}.
     * It captures the target object, the method metadata, and the arguments to
     * evaluate the dynamic 'when' condition defined in the pointcut settings.</p>
     *
     * @param obj        the instance on which the method is invoked (null for static methods).
     * @param method     the reflected {@link Method} being executed (used for logging).
     * @param args       array of arguments passed to the method.
     * @param pointcutId unique identifier injected by the instrumentation engine to
     *                   retrieve specific pointcut configurations.
     */
    @Advice.OnMethodEnter
    public static void enter(@Advice.This(optional = true) Object obj,
                             @Advice.Origin Method method,
                             @Advice.AllArguments Object[] args,
                             @PointcutId int pointcutId) {
        MethodCountingTracker.startTask(pointcutId, obj, method, args);
    }

    /**
     * Invoked when the instrumented method exits, whether normally or by throwing an exception.
     *
     * <p>The {@code onThrowable = Throwable.class} attribute is critical here: it ensures
     * that the task counter is decremented even if the method fails. Without this,
     * a thrown exception could cause the test to block indefinitely, waiting for
     * a task that will never "finish" normally.</p>
     *
     * @param obj        the instance on which the method was invoked.
     * @param method     the reflected {@link Method} that finished execution.
     * @param args       array of arguments passed to the method.
     * @param pointcutId unique identifier associated with this instrumentation point.
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(@Advice.This(optional = true) Object obj,
                            @Advice.Origin Method method,
                            @Advice.AllArguments Object[] args,
                            @PointcutId int pointcutId) {
        MethodCountingTracker.endTask(pointcutId, obj, method, args);
    }
}
