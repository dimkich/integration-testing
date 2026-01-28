package io.github.dimkich.integration.testing.execution.junit;

import lombok.Getter;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

import java.util.HashMap;
import java.util.Map;

/**
 * JUnit Platform {@link LauncherSessionListener} that manages a shared {@link ExecutionListener}
 * instance for the duration of a launcher session.
 * <p>
 * The listener is created lazily when the first launcher session is opened and registered both as
 * a discovery and execution listener on the {@link org.junit.platform.launcher.Launcher}. The
 * most recently created listener is exposed via {@link #getExecutionListener()} for use in tests.
 */
public class SessionListener implements LauncherSessionListener {
    /**
     * The {@link ExecutionListener} associated with the last opened launcher session.
     */
    @Getter
    private static ExecutionListener executionListener;

    /**
     * Internal map that holds a dedicated {@link ExecutionListener} instance for each
     * {@link SessionListener}. This ensures that every session listener reuses its own listener
     * across multiple invocations of {@link #launcherSessionOpened(LauncherSession)}.
     */
    private static final Map<SessionListener, ExecutionListener> listenerMap = new HashMap<>();

    /**
     * Callback invoked by the JUnit Platform when a new {@link LauncherSession} is opened.
     * <p>
     * If this {@code SessionListener} does not yet have an associated {@link ExecutionListener},
     * a new instance is created and registered with the launcher as both a discovery and
     * execution listener. The associated listener is then stored in {@link #listenerMap} and
     * assigned to {@link #executionListener}.
     *
     * @param session the newly opened {@link LauncherSession}
     */
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
