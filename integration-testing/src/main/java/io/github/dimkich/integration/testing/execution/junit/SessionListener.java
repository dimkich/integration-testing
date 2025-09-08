package io.github.dimkich.integration.testing.execution.junit;

import lombok.Getter;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

import java.util.HashMap;
import java.util.Map;

public class SessionListener implements LauncherSessionListener {
    @Getter
    private static ExecutionListener executionListener;
    private static final Map<SessionListener, ExecutionListener> listenerMap = new HashMap<>();

    @Override
    public void launcherSessionOpened(LauncherSession session) {
        ExecutionListener listener = listenerMap.get(this);
        if (listener == null) {
            listener = new ExecutionListener();
            session.getLauncher().registerLauncherDiscoveryListeners(listener);
            session.getLauncher().registerTestExecutionListeners(listener);
            listenerMap.put(this, listener);
        }
        executionListener = listener;
    }
}
