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

public class ExecutionListener implements TestExecutionListener, LauncherDiscoveryListener {
    private Map<UniqueId, UniqueIdCollector> lastTests;
    private UniqueId rootId;

    @Override
    public void launcherDiscoveryFinished(LauncherDiscoveryRequest request) {
        lastTests = request.getSelectorsByType(UniqueIdSelector.class).stream()
                .map(UniqueIdCollector::new)
                .collect(Collectors.toMap(UniqueIdCollector::getId, Function.identity(), UniqueIdCollector::max));
    }

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        if (testIdentifier.getUniqueIdObject().getSegments().size() == 3) {
            rootId = testIdentifier.getUniqueIdObject();
        }
    }

    public Path getTestFilePath() {
        List<UniqueId.Segment> segments = rootId.getSegments();
        return Path.of(segments.get(1).getValue()).resolve(segments.get(2).getValue());
    }

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
