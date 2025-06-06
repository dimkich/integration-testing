package io.github.dimkich.integration.testing.execution.junit;

import lombok.Getter;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.UniqueIdSelector;
import org.junit.platform.launcher.LauncherDiscoveryListener;
import org.junit.platform.launcher.LauncherDiscoveryRequest;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DiscoveryListener implements LauncherDiscoveryListener {
    @Getter
    private static DiscoveryListener instance;

    private final Set<UniqueId> lastTestCases = new HashSet<>();

    public DiscoveryListener() {
        instance = this;
    }

    public boolean isLastTestCase(UniqueId uniqueId) {
        return lastTestCases.contains(uniqueId);
    }

    @Override
    public void launcherDiscoveryFinished(LauncherDiscoveryRequest request) {
        request.getSelectorsByType(UniqueIdSelector.class).stream()
                .map(UniqueIdCollector::new)
                .collect(Collectors.toMap(Function.identity(), Function.identity(), UniqueIdCollector::max))
                .values().stream()
                .map(UniqueIdCollector::getId)
                .forEach(lastTestCases::add);
    }
}
