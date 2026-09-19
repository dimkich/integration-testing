package io.github.dimkich.integration.testing.execution.hook;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;

public class ExitAction implements MethodAction {
    @Override
    public void execute(Object target, Executable method, Object[] args, Object returnValue, Throwable thrown) {
        HookEvents.add(String.format("exit method=%s target=%s return=%s thrown=%s",
                method instanceof Constructor ? "<init>" : method.getName(),
                target == null ? "null" : target.getClass().getSimpleName(),
                returnValue,
                thrown == null ? "null" : thrown.getClass().getSimpleName() + ":" + thrown.getMessage()));
    }
}
