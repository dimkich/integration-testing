package io.github.dimkich.integration.testing.execution.mokito;

import lombok.Getter;
import lombok.Setter;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import org.mockito.internal.util.concurrent.DetachedThreadLocal;

import java.lang.reflect.Method;

public class DetachedThreadLocalAdvice {
    public static final Thread thread = new Thread("Mockito global thread");
    @Setter
    @Getter
    private static boolean global;

    @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
    public static boolean enter(@Advice.Origin Method method,
                                @Advice.AllArguments Object[] args) {
        if (DetachedThreadLocalAdvice.isGlobal() && args.length > 0) {
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
        return DetachedThreadLocalAdvice.isGlobal()
                && ("set".equals(method.getName()) || "clear".equals(method.getName()));
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(
            @Advice.This Object obj,
            @Advice.Origin Method method,
            @Advice.Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object returnValue,
            @Advice.AllArguments Object[] args,
            @Advice.Thrown Throwable throwable
    ) {
        if (DetachedThreadLocalAdvice.isGlobal()) {
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
}
