package io.github.dimkich.integration.testing.execution.junit;

import io.github.dimkich.integration.testing.Test;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.UniqueIdSelector;
import org.junit.platform.launcher.LauncherDiscoveryListener;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * JUnit Platform listener that correlates launcher discovery and execution events
 * with the framework's {@link Test} hierarchy.
 * <p>
 * Implements both {@link LauncherDiscoveryListener} and {@link TestExecutionListener}
 * to:
 * <ul>
 *   <li>Build a map of root {@link UniqueId}s to {@link UniqueIdCollector}s from
 *       {@link UniqueIdSelector}s after discovery</li>
 *   <li>Track the current root test's {@link UniqueId} when execution starts</li>
 *   <li>Expose the test file path derived from the root id and the "last" selected
 *       test in the hierarchy for use by {@link io.github.dimkich.integration.testing.execution.TestExecutor}</li>
 * </ul>
 * <p>
 * Typically registered per launcher session via {@link SessionListener} and
 * obtained through {@link SessionListener#getExecutionListener()}.
 *
 * @see SessionListener
 * @see UniqueIdCollector
 * @see Test
 */
public class ExecutionListener implements TestExecutionListener, LauncherDiscoveryListener {
    /**
     * Map of root {@link UniqueId} to collector for the lexicographically last selected test.
     */
    private Map<UniqueId, UniqueIdCollector> lastTests;
    /** Current root test's {@link UniqueId} (engine + class + method/container). */
    private UniqueId rootId;

    /**
     * After discovery, builds a map from each root {@link UniqueId} to a
     * {@link UniqueIdCollector} for the lexicographically greatest selected
     * test under that root (when multiple selectors target the same root).
     *
     * @param request the discovery request containing {@link UniqueIdSelector}s
     */
    @Override
    public void launcherDiscoveryFinished(LauncherDiscoveryRequest request) {
        lastTests = request.getSelectorsByType(UniqueIdSelector.class).stream()
                .map(UniqueIdCollector::new)
                .collect(Collectors.toMap(UniqueIdCollector::getId, Function.identity(), UniqueIdCollector::max));
    }

    /**
     * Stores the test's root {@link UniqueId} when execution starts for a
     * top-level identifier (exactly three segments: engine, class, method or container).
     *
     * @param testIdentifier the identifier of the test whose execution has started
     */
    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        if (testIdentifier.getUniqueIdObject().getSegments().size() == 3) {
            rootId = testIdentifier.getUniqueIdObject();
        }
    }

    /**
     * Returns the file path for the current root test, derived from the root
     * {@link UniqueId}'s second and third segments (class and method/container).
     * <p>
     * Used to resolve the test-specific temp directory under the assertion result dir.
     *
     * @return path formed as {@code segment[1] / segment[2]} of the root id
     */
    public Path getTestFilePath() {
        List<UniqueId.Segment> segments = rootId.getSegments();
        return Path.of(segments.get(1).getValue()).resolve(segments.get(2).getValue());
    }

    /**
     * Locates the "last" selected test in the hierarchy under the given root,
     * using the {@link UniqueIdCollector} index sequence for the current root.
     * <p>
     * Navigates from {@code root} through {@link Test#getSubTests()} by each
     * index in the collector; if no collector exists for the root, returns
     * the root itself.
     *
     * @param root the root {@link Test} of the hierarchy
     * @return the leaf test corresponding to the last selected id, or {@code root} if none
     */
    public Test findLastTest(Test root) {
        Test test = root;
        UniqueIdCollector collector = lastTests.get(rootId);
        if (collector != null) {
            for (Integer index : collector.getIndexes()) {
                test = test.getSubTests().get(index);
            }
        }
        return test;
    }
}
