package io.github.dimkich.integration.testing.execution.junit;

import io.github.dimkich.integration.testing.util.ByteBuddyUtils;
import lombok.Getter;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

import java.util.HashMap;
import java.util.Map;

/**
 * A JUnit Platform {@link LauncherSessionListener} that orchestrates the lifecycle of
 * {@link ExecutionListener} and ensures proper bootstrapping for Byte Buddy advices.
 * <p>
 * This listener performs two critical roles:
 * <ol>
 *     <li><b>Class Bootstrapping:</b> In its static initializer, it forces key utility and
 *     advice classes into the Boot ClassLoader. This is mandatory for instrumenting system
 *     classes or classes where the advice must be visible globally across all class loaders.</li>
 *     <li><b>Session Management:</b> It maintains a mapping between launcher sessions and
 *     execution listeners, ensuring that each session has a consistent, registered observer
 *     for both test discovery and execution phases.</li>
 * </ol>
 * <p>
 * The most recently active {@link ExecutionListener} is globally accessible via
 * {@link #getExecutionListener()} to bridge JUnit execution state with test logic.
 */
public class SessionListener implements LauncherSessionListener {
    /**
     * Bootstraps essential instrumentation classes.
     * <p>
     * Classes like {@code JavaTimeAdvice} and various trackers must reside in the Boot
     * ClassLoader to avoid {@link NoClassDefFoundError} when they are injected into
     * classes loaded by the bootstrap or extension class loaders.
     */
    static {
        String basePath = "io.github.dimkich.integration.testing.";
        ByteBuddyUtils.moveClassToBootClassLoader(
                basePath + "date.time.JavaTimeAdvice",
                basePath + "expression.PointcutSettings",
                basePath + "expression.PointcutRegistry",
                basePath + "wait.completion.future.like.FutureLikeTracker",
                basePath + "wait.completion.method.counting.MethodCountingTracker",
                basePath + "wait.completion.method.pair.MethodPairTracker",
                basePath + "wait.completion.queue.like.QueueLikeTracker",
                basePath + "wait.completion.queue.like.QueueLikeTracker$IdentityWeakReference"
        );
    }

    /**
     * The {@link ExecutionListener} instance associated with the most recently
     * initialized session.
     */
    @Getter
    private static ExecutionListener executionListener;

    /**
     * Internal registry to maintain a 1:1 mapping between session listeners and
     * their respective execution observers.
     */
    private static final Map<SessionListener, ExecutionListener> listenerMap = new HashMap<>();

    /**
     * Invoked by the JUnit Platform when a session starts.
     * <p>
     * Initializes a new {@link ExecutionListener} for the session if it doesn't exist,
     * and registers it as both a <i>Discovery Listener</i> (to intercept test scanning)
     * and a <i>Test Execution Listener</i> (to monitor test lifecycle events).
     *
     * @param session the current JUnit launcher session.
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
