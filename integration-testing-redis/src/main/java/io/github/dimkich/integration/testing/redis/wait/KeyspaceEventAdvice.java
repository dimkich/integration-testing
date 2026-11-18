package io.github.dimkich.integration.testing.redis.wait;

import io.github.dimkich.integration.testing.expression.PointcutId;
import net.bytebuddy.asm.Advice;

/** Counts completed invocations of a keyspace notification handler selected by a pointcut. */
public class KeyspaceEventAdvice {

    /**
     * Counts the call on exit, not on entry: the handler must have finished its work (for example
     * removed a key from an index) before the wait may complete. Failed handlers are counted too,
     * otherwise the wait would hang on an exception.
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(@PointcutId int pointcutId) {
        KeyspaceEventTracker.onCall(pointcutId);
    }
}
