package io.github.dimkich.integration.testing.wait.completion.queue.like;

import io.github.dimkich.integration.testing.expression.PointcutId;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

/**
 * ByteBuddy advice responsible for intercepting the instantiation or return of
 * "queue-like" service instances.
 *
 * <p>This advice is typically applied to constructors (to capture the newly created
 * {@code this} instance) or factory methods (to capture the {@code returned} object).
 * Once an instance is captured, it is handed over to the {@link QueueLikeTracker}
 * to monitor its internal task or queue size during test execution.</p>
 *
 * <h3>Interception Logic:</h3>
 * <ul>
 *   <li><b>Context Selection:</b> If {@code @Advice.Return} is not null (factory method),
 *       it is treated as the service. Otherwise, {@code @Advice.This} is used.</li>
 *   <li><b>Lazy Registration:</b> Instances are registered only upon successful
 *       method exit to ensure only fully initialized objects are tracked.</li>
 * </ul>
 *
 * @see QueueLikeTracker
 * @see QueueLikeWaitCompletion
 */
public class QueueLikeAdvice {

    /**
     * Invoked when the instrumented constructor or factory method exits.
     *
     * <p>Identifies the service instance and registers it for state polling.
     * The use of {@code typing = Assigner.Typing.DYNAMIC} for the return value
     * ensures compatibility with various return types defined in pointcuts.</p>
     *
     * @param thiz       the intercepted instance (captured for constructors).
     * @param returnObj  the intercepted return value (captured for factory methods).
     * @param args       the arguments passed to the constructor/method (used for conditional 'when' logic).
     * @param pointcutId the unique ID injected to link this call with its {@link QueueLikeAwait} configuration.
     */
    @Advice.OnMethodExit
    public static void exit(
            @Advice.This(optional = true) Object thiz,
            @Advice.Return(typing = Assigner.Typing.DYNAMIC) Object returnObj,
            @Advice.AllArguments Object[] args,
            @PointcutId int pointcutId) {

        // Priority is given to the returned object (factory pattern)
        Object service = (returnObj != null) ? returnObj : thiz;

        if (service != null) {
            QueueLikeTracker.addService(pointcutId, service, args);
        }
    }
}
