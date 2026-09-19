package io.github.dimkich.integration.testing.execution.mockito;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import org.mockito.internal.util.concurrent.DetachedThreadLocal;

import java.lang.reflect.Method;

/**
 * ByteBuddy {@link Advice} that redirects calls to Mockito's {@link DetachedThreadLocal}
 * so that all operations are performed against a single shared {@link #thread}, instead
 * of the current thread.
 * <p>
 * Invocations of selected {@link DetachedThreadLocal} methods are rewritten so that:
 * <ul>
 *     <li>Thread-based arguments are replaced with the special {@link #thread} instance.</li>
 *     <li>Reads and writes are executed against the backing map entry keyed by {@link #thread}.</li>
 * </ul>
 * This allows Mockito state to be shared across threads in integration tests while still
 * using the standard {@link DetachedThreadLocal} API.
 * <p>
 * This advice is only active while the {@link MockitoGlobalPlugin} transformer is installed.
 */
public class DetachedThreadLocalAdvice {

    /**
     * Special thread instance used as a global key in {@link DetachedThreadLocal}
     * backing maps instead of the real current thread.
     */
    @SuppressWarnings("InstantiatingAThreadWithDefaultRunMethod")
    public static final Thread thread = new Thread("Mockito global thread");

    /**
     * Method entry advice that optionally rewrites arguments for
     * {@link DetachedThreadLocal} methods and decides whether to skip the original call.
     *
     * @param method the intercepted method
     * @param args   the method arguments (may be modified in-place)
     * @return {@code true} to skip the original method invocation, {@code false} to proceed
     * as normal
     */
    @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
    public static boolean enter(@Advice.Origin Method method,
                                @Advice.AllArguments Object[] args) {
        if (args.length > 0) {
            switch (method.getName()) {
                case "get":
                case "initialValue":
                case "define":
                case "fetchFrom":
                case "pushTo":
                    args[0] = thread;
                    break;
            }
        }
        return "set".equals(method.getName()) || "clear".equals(method.getName());
    }

    /**
     * Method exit advice that replaces or updates the result of
     * {@link DetachedThreadLocal} operations.
     *
     * @param obj         the intercepted {@link DetachedThreadLocal} instance
     * @param method      the intercepted method
     * @param returnValue the method return value (may be reassigned)
     * @param args        the method arguments
     */
    @SuppressWarnings({"ParameterCanBeLocal", "UnusedAssignment", "unchecked"})
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(
            @Advice.This Object obj,
            @Advice.Origin Method method,
            @Advice.Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object returnValue,
            @Advice.AllArguments Object[] args
    ) {
        DetachedThreadLocal<Object> detachedThreadLocal = (DetachedThreadLocal<Object>) obj;
        if (args.length == 0 && "get".equals(method.getName())) {
            returnValue = detachedThreadLocal.getBackingMap().get(thread);
        } else if ("set".equals(method.getName())) {
            detachedThreadLocal.getBackingMap().put(thread, args[0]);
        } else if ("clear".equals(method.getName())) {
            detachedThreadLocal.getBackingMap().remove(thread);
        }
    }
}
