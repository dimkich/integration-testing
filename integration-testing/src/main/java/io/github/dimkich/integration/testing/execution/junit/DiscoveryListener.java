package io.github.dimkich.integration.testing.execution.junit;

import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.UniqueIdSelector;
import org.junit.platform.launcher.LauncherDiscoveryListener;
import org.junit.platform.launcher.LauncherDiscoveryRequest;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DiscoveryListener implements LauncherDiscoveryListener {
    private static final Deque<DiscoveryListener> instances = new ArrayDeque<>();

    private final Set<UniqueId> lastTests = new HashSet<>();

    public DiscoveryListener() {
        instances.addLast(this);
    }

    public static DiscoveryListener getLast() {
        return instances.getLast();
    }

    public static void removeLast() {
        instances.removeLast();
    }

    public boolean isLastTest(UniqueId uniqueId) {
        return lastTests.contains(uniqueId);
    }

    @Override
    public void launcherDiscoveryFinished(LauncherDiscoveryRequest request) {
        request.getSelectorsByType(UniqueIdSelector.class).stream()
                .map(UniqueIdCollector::new)
                .collect(Collectors.toMap(Function.identity(), Function.identity(), UniqueIdCollector::max))
                .values().stream()
                .map(UniqueIdCollector::getId)
                .forEach(lastTests::add);
    }
}
