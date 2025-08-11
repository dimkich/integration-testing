
package io.github.dimkich.integration.testing.execution;

import io.github.dimkich.integration.testing.execution.junit.JunitExecutable;
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
    private final String name;
    private final Set<String> methods;
    private final MockInvokeProperties properties;
    private final JunitExecutable junitExecutable;
    private final boolean isSpy;

    private int nestedCalls = 0;

    @Override
    public Object answer(InvocationOnMock invocation) throws Throwable {
        if (!junitExecutable.isTestRunning() || methods != null && !methods.contains(invocation.getMethod().getName())) {
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
            return mi.getCurrentResult();
        }
        return mi.getCurrentResult();
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
