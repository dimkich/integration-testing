
package io.github.dimkich.integration.testing.execution;

import eu.ciechanowiec.sneakyfun.SneakyRunnable;
import eu.ciechanowiec.sneakyfun.SneakySupplier;
import io.github.dimkich.integration.testing.execution.junit.JunitExecutable;
import io.github.sugarcubes.cloner.Cloner;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
public class MockAnswer implements Answer<Object> {
    @Getter
    private static boolean enabled = false;

    private final String name;
    private final Set<String> methods;
    private final MockInvokeProperties properties;
    private final JunitExecutable junitExecutable;
    private final Cloner cloner;
    private final boolean isSpy;
    private final boolean cloneArgsAndResult;

    private int nestedCalls = 0;

    public static <T, E extends Exception> T enable(SneakySupplier<T, E> supplier) throws E {
        enabled = true;
        try {
            return supplier.get();
        } finally {
            enabled = false;
        }
    }

    public static <E extends Exception> void enable(SneakyRunnable<E> runnable) throws E {
        enabled = true;
        try {
            runnable.run();
        } finally {
            enabled = false;
        }
    }

    @Override
    public Object answer(InvocationOnMock invocation) throws Throwable {
        if (!enabled || methods != null && !methods.contains(invocation.getMethod().getName())) {
            return invocation.callRealMethod();
        }
        List<Object> args = Arrays.stream(invocation.getArguments()).toList();
        args = args.isEmpty() ? null : args;
        MockInvoke mi = junitExecutable.search(name, invocation.getMethod().getName(), args);

        boolean mockInvokeFound = mi != null;
        if (mi == null) {
            mi = new MockInvoke().setName(name).setMethod(invocation.getMethod().getName()).setArg(args);
        }
        if (addMockInvoke(mockInvokeFound)) {
            if (cloneArgsAndResult) {
                mi.setArg(mi.getArg().stream().map(cloner::clone).toList());
            }
            junitExecutable.addMockInvoke(mi);
        }
        if (callRealMethod(mockInvokeFound)) {
            nestedCalls++;
            try {
                Object result = invocation.callRealMethod();
                mi.addResult(result);
            } catch (Throwable e) {
                mi.addException(e);
            } finally {
                nestedCalls--;
            }
        }
        mi.tryThrowException();
        if (returnMock(mockInvokeFound)) {
            if (invocation.getMethod().getReturnType().equals(Void.TYPE)) {
                return null;
            }
            mi.addResult(Mockito.mock(invocation.getMethod().getReturnType(), Answers.RETURNS_DEEP_STUBS));
        }
        return cloneArgsAndResult ? cloner.clone(mi.getCurrentResult()) : mi.getCurrentResult();
    }

    private boolean callRealMethod(boolean mockInvokeFound) {
        return (isSpy && !mockInvokeFound) || properties.isMockAlwaysCallRealMethods()
                || properties.isMockCallRealMethodsOnNoData() && !mockInvokeFound;
    }

    private boolean addMockInvoke(boolean mockInvokeFound) {
        return nestedCalls == 0 && (!mockInvokeFound || properties.isMockAlwaysCallRealMethods())
                && (!isSpy || properties.isSpyCreateData());
    }

    private boolean returnMock(boolean mockInvokeFound) {
        return properties.isMockReturnMockOnNoData() && !mockInvokeFound && !callRealMethod(mockInvokeFound);
    }
}
