package io.github.dimkich.integration.testing.execution.junit;

import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

import java.util.ArrayDeque;
import java.util.Deque;

public class SessionListener implements LauncherSessionListener {
    private final static Deque<ExecutionListener> executions = new ArrayDeque<>();

    public static ExecutionListener getExecutionListener() {
        return executions.getLast();
    }

    @Override
    public void launcherSessionOpened(LauncherSession session) {
        ExecutionListener executionListener = new ExecutionListener();
        session.getLauncher().registerLauncherDiscoveryListeners(executionListener);
        session.getLauncher().registerTestExecutionListeners(executionListener);
        executions.addLast(executionListener);
    }

    @Override
    public void launcherSessionClosed(LauncherSession session) {
        executions.removeLast();
    }
}
