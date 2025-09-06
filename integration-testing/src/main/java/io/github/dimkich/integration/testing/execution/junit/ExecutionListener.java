package io.github.dimkich.integration.testing.execution.junit;

import lombok.Getter;
import lombok.SneakyThrows;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.UniqueIdSelector;
import org.junit.platform.launcher.LauncherDiscoveryListener;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ExecutionListener implements TestExecutionListener, LauncherDiscoveryListener {
    @Getter
    private final Deque<JunitTestInfo> junitTests = new ArrayDeque<>();
    private final Set<UniqueId> lastTests = new HashSet<>();

    @Override
    public void launcherDiscoveryFinished(LauncherDiscoveryRequest request) {
        request.getSelectorsByType(UniqueIdSelector.class).stream()
                .map(UniqueIdCollector::new)
                .collect(Collectors.toMap(Function.identity(), Function.identity(), UniqueIdCollector::max))
                .values().stream()
                .map(UniqueIdCollector::getId)
                .forEach(lastTests::add);
    }

    @Override
    @SneakyThrows
    public void executionStarted(TestIdentifier testIdentifier) {
        UniqueId id = testIdentifier.getUniqueIdObject();
        if (id.getSegments().size() > 2) {
            junitTests.addLast(new JunitTestInfo(id, lastTests.contains(id)));
        }
    }

    @Override
    @SneakyThrows
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
        if (testIdentifier.getUniqueIdObject().getSegments().size() > 2) {
            junitTests.removeLast();
        }
    }
}
