
package io.github.dimkich.integration.testing.execution;

import eu.ciechanowiec.sneakyfun.SneakyRunnable;
import eu.ciechanowiec.sneakyfun.SneakySupplier;
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

/**
 * Mockito {@link Answer} implementation used by integration-testing to intercept method invocations,
 * record {@link StubCall} metadata and optionally call real methods or return stubbed results.
 * <p>
 * Interception is globally controlled by the static {@link #enabled} flag which is switched on only
 * within the {@link #enable(SneakySupplier)} and {@link #enable(SneakyRunnable)} scopes.
 * Outside of these scopes the answer delegates directly to {@link InvocationOnMock#callRealMethod()}.
 */
@RequiredArgsConstructor
public class StubAnswer implements Answer<Object> {
    /**
     * Global flag that enables interception when {@code true}.
     * It is managed only by {@link #enable(SneakySupplier)} and {@link #enable(SneakyRunnable)}.
     */
    @Getter
    private static boolean enabled = false;

    private final String name;
    private final Set<String> methods;
    private final StubProperties properties;
    private final TestExecutor testExecutor;
    private final Cloner cloner;
    private final boolean isSpy;
    private final boolean cloneArgsAndResult;

    /**
     * Counter for nested calls to avoid recording internal recursive invocations.
     */
    private int nestedCalls = 0;

    /**
     * Executes the supplied code with interception globally enabled.
     * <p>
     * The {@link #enabled} flag is always reset back to {@code false} after execution.
     *
     * @param supplier code to execute while interception is enabled
     * @param <T>      result type
     * @param <E>      checked exception type that the supplier may throw
     * @return value returned by the supplied code
     * @throws E any exception thrown by the supplier
     */
    public static <T, E extends Exception> T enable(SneakySupplier<T, E> supplier) throws E {
        enabled = true;
        try {
            return supplier.get();
        } finally {
            enabled = false;
        }
    }

    /**
     * Executes the given runnable with interception globally enabled.
     * <p>
     * The {@link #enabled} flag is always reset back to {@code false} after execution.
     *
     * @param runnable code to execute while interception is enabled
     * @param <E>      checked exception type that the runnable may throw
     * @throws E any exception thrown by the runnable
     */
    public static <E extends Exception> void enable(SneakyRunnable<E> runnable) throws E {
        enabled = true;
        try {
            runnable.run();
        } finally {
            enabled = false;
        }
    }

    /**
     * Handles a Mockito invocation, optionally delegating to the real method and/or storing
     * invocation data in {@link StubCall} instances according to {@link StubProperties}.
     *
     * @param invocation current stubbed invocation
     * @return value to be returned from the stubbed method
     * @throws Throwable any error thrown by the real method
     */
    @Override
    public Object answer(InvocationOnMock invocation) throws Throwable {
        if (!enabled || methods != null && !methods.contains(invocation.getMethod().getName())) {
            return invocation.callRealMethod();
        }
        List<Object> args = Arrays.stream(invocation.getArguments()).toList();
        args = args.isEmpty() ? null : args;
        StubCall stubCall = testExecutor.search(name, invocation.getMethod().getName(), args);
        if (stubCall != null && stubCall.isDisabled()) {
            return invocation.callRealMethod();
        }

        boolean stubCallFound = stubCall != null;
        if (stubCall == null) {
            stubCall = new StubCall().setName(name).setMethod(invocation.getMethod().getName()).setArg(args)
                    .setUsed(true);
        }
        if (addStubCall(stubCallFound)) {
            if (cloneArgsAndResult) {
                stubCall.setArg(stubCall.getArg().stream().map(cloner::clone).toList());
            }
            testExecutor.addStubCall(stubCall);
        }
        if (callRealMethod(stubCallFound)) {
            nestedCalls++;
            try {
                Object result = invocation.callRealMethod();
                stubCall.addResult(result);
            } catch (Throwable e) {
                stubCall.addException(e);
            } finally {
                nestedCalls--;
            }
        }
        stubCall.tryThrowException();
        if (returnMock(stubCallFound)) {
            if (invocation.getMethod().getReturnType().equals(Void.TYPE)) {
                return null;
            }
            stubCall.addResult(Mockito.mock(invocation.getMethod().getReturnType(), Mockito.withSettings()
                    .defaultAnswer(Answers.RETURNS_DEEP_STUBS).stubOnly()));
        }
        return cloneArgsAndResult ? cloner.clone(stubCall.getCurrentResult()) : stubCall.getCurrentResult();
    }

    private boolean callRealMethod(boolean stubCallFound) {
        return (isSpy && !stubCallFound) || properties.isAlwaysCallRealMethods()
                || properties.isCallRealMethodsOnNoData() && !stubCallFound;
    }

    private boolean addStubCall(boolean stubCallFound) {
        return nestedCalls == 0 && (!stubCallFound || properties.isAlwaysCallRealMethods())
                && (!isSpy || properties.isSpyCreateData());
    }

    private boolean returnMock(boolean stubCallFound) {
        if (stubCallFound) {
            return false;
        }
        return properties.isReturnMockOnNoData() && !callRealMethod(false);
    }
}
