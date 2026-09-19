package io.github.dimkich.integration.testing.execution.hook;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.util.Arrays;

public class EnterAction implements MethodAction {
    @Override
    public void execute(Object target, Executable method, Object[] args, Object returnValue, Throwable thrown) {
        HookEvents.add(String.format("enter method=%s target=%s args=%s",
                method instanceof Constructor ? "<init>" : method.getName(),
                target == null ? "null" : target.getClass().getSimpleName(),
                Arrays.toString(args)));
    }
}
