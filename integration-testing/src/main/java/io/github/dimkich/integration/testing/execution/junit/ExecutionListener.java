package io.github.dimkich.integration.testing.execution.junit;

import lombok.Getter;
import lombok.SneakyThrows;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class ExecutionListener implements TestExecutionListener {
    private static final Deque<ExecutionListener> instances = new ArrayDeque<>();
    @Getter
    private final Deque<JunitTestInfo> junitTests = new ArrayDeque<>();
    private DiscoveryListener discoveryListener;

    public ExecutionListener() {
        instances.addLast(this);
    }

    public static ExecutionListener getLast() {
        return instances.getLast();
    }

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        discoveryListener = DiscoveryListener.getLast();
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        discoveryListener = null;
        DiscoveryListener.removeLast();
        instances.removeLast();
    }

    @Override
    @SneakyThrows
    public void executionStarted(TestIdentifier testIdentifier) {
        UniqueId id = testIdentifier.getUniqueIdObject();
        List<UniqueId.Segment> segments = id.getSegments();
        if (segments.size() > 2) {
            UniqueId.Segment segment = segments.get(segments.size() - 1);
            switch (segment.getType()) {
                case "test-factory":
                    junitTests.addLast(new JunitTestInfo(id, discoveryListener.isLastTest(id)));
                    break;
                case "dynamic-container":
                case "dynamic-test":
                    junitTests.getLast().setSubTestIndex(Integer.parseInt(segment.getValue().substring(1)) - 1);
                    junitTests.addLast(new JunitTestInfo(id, discoveryListener.isLastTest(id)));
            }
        }
    }

    @Override
    @SneakyThrows
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
        if (testIdentifier.getUniqueIdObject().getSegments().size() > 2 && !junitTests.isEmpty()) {
            junitTests.removeLast();
        }
    }
}
