package io.github.dimkich.integration.testing.execution.hook;

import java.lang.reflect.Executable;

/**
 * Interface for executing custom actions when SUT methods are intercepted
 * by {@link OnMethodEnter} or {@link OnMethodExit}.
 *
 * <p>Implementations must have a public no-args constructor. An action is executed
 * in the context of the intercepted call and can inspect (and mutate) the target,
 * its arguments and, for exit hooks, the returned value or the thrown exception.</p>
 */
public interface MethodAction {
    /**
     * Executes the action for an intercepted method or constructor call.
     *
     * @param target the instance on which the intercepted method was invoked,
     *               or {@code null} for static methods
     * @param method the intercepted method or constructor
     * @param args the arguments of the intercepted call; changes are visible to the caller
     * @param returnValue the value returned by the intercepted method, or {@code null}
     *                    for enter hooks and constructors
     * @param thrown the exception thrown by the intercepted method, or {@code null}
     *               if it completed normally
     * @throws Exception if the action fails; the exception is logged and swallowed
     */
    void execute(Object target, Executable method, Object[] args, Object returnValue, Throwable thrown) throws Exception;
}
