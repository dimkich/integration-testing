package io.github.dimkich.integration.testing.execution.hook;

import io.github.dimkich.integration.testing.expression.PointcutId;
import io.github.dimkich.integration.testing.expression.PointcutRegistry;
import io.github.dimkich.integration.testing.expression.PointcutSettings;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

import java.lang.reflect.Executable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ByteBuddy advice for hooks declared via {@link OnMethodEnter} and {@link OnMethodExit}.
 *
 * <p>Both advices resolve pointcut settings by the injected {@link PointcutId}, evaluate
 * the dynamic 'when' condition and delegate to the configured {@link MethodAction}.</p>
 */
public class MethodHookAdvice {
    /**
     * Logger accessed from inlined advice code, therefore public.
     */
    public static final Logger log = Logger.getLogger(MethodHookAdvice.class.getName());

    /**
     * Entry advice: resolves pointcut settings and executes the configured
     * {@link MethodAction} after entering the intercepted method or constructor.
     */
    public static class Enter {
        /**
         * Entry advice method inlined by Byte Buddy.
         *
         * @param obj the intercepted instance, {@code null} for static methods
         * @param method the intercepted method or constructor
         * @param args the call arguments
         * @param pointcutId id of the pointcut used to resolve the settings
         */
        @Advice.OnMethodEnter
        public static void enter(@Advice.This(optional = true) Object obj,
                                 @Advice.Origin Executable method,
                                 @Advice.AllArguments Object[] args,
                                 @PointcutId int pointcutId) {
            PointcutSettings settings = PointcutRegistry.get(pointcutId);
            if (settings.checkWhen(obj, args) && settings.getAction() != null) {
                try {
                    settings.getAction().execute(obj, method, args, null, null);
                } catch (Exception e) {
                    log.log(Level.FINE, "Method hook enter failed for " + method, e);
                }
            }
        }
    }

    /**
     * Exit advice: resolves pointcut settings and executes the configured
     * {@link MethodAction} when the intercepted method exits, both normally and
     * with an exception.
     */
    public static class Exit {
        /**
         * Exit advice method inlined by Byte Buddy.
         *
         * @param obj the intercepted instance, {@code null} for static methods
         * @param method the intercepted method
         * @param returnValue the value returned by the method, {@code null} if it threw
         * @param thrown the exception thrown by the method, {@code null} if it completed normally
         * @param args the call arguments
         * @param pointcutId id of the pointcut used to resolve the settings
         */
        @Advice.OnMethodExit(onThrowable = Throwable.class)
        public static void exit(@Advice.This(optional = true) Object obj,
                                @Advice.Origin Executable method,
                                @Advice.Return(typing = Assigner.Typing.DYNAMIC) Object returnValue,
                                @Advice.Thrown Throwable thrown,
                                @Advice.AllArguments Object[] args,
                                @PointcutId int pointcutId) {
            PointcutSettings settings = PointcutRegistry.get(pointcutId);
            if (settings.checkWhen(obj, args) && settings.getAction() != null) {
                try {
                    settings.getAction().execute(obj, method, args, returnValue, thrown);
                } catch (Exception e) {
                    log.log(Level.FINE, "Method hook exit failed for " + method, e);
                }
            }
        }
    }
}
