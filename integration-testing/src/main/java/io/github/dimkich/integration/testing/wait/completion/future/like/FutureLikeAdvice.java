package io.github.dimkich.integration.testing.wait.completion.future.like;

import io.github.dimkich.integration.testing.expression.PointcutId;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

/**
 * ByteBuddy advice for intercepting the creation or return of "future-like" objects.
 * <p>
 * This advice is applied to {@code @OnMethodExit}. It identifies the newly created
 * asynchronous task (either 'this' for constructors or the 'returned' value for
 * factory methods) and registers it in the {@link FutureLikeTracker}.
 * </p>
 */
public class FutureLikeAdvice {
    /**
     * Invoked when the intercepted method exits.
     *
     * @param thiz       the target instance (used for constructors).
     * @param returned   the returned value (used for factory methods).
     * @param args       arguments passed to the method, used for conditional matching.
     * @param pointcutId the unique ID injected by {@link io.github.dimkich.integration.testing.expression.PointcutMatch}.
     */
    @Advice.OnMethodExit
    public static void exit(
            @Advice.This(optional = true) Object thiz,
            @Advice.Return(typing = Assigner.Typing.DYNAMIC) Object returned,
            @Advice.AllArguments Object[] args,
            @PointcutId int pointcutId) {
        Object obj = (thiz != null) ? thiz : returned;
        if (obj != null) {
            FutureLikeTracker.addTask(pointcutId, obj, args);
        }
    }
}
