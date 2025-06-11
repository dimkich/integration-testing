package io.github.dimkich.integration.testing.execution.junit;

import io.github.dimkich.integration.testing.TestCase;
import io.github.dimkich.integration.testing.execution.TestExecutor;
import io.github.dimkich.integration.testing.util.ConsumerWithException;
import lombok.*;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.launcher.*;

public class ExecutionListener implements TestExecutionListener {
    @Getter
    private static ExecutionListener instance;
    @Setter
    private TestCase root;
    @Setter
    private TestExecutor testExecutor;
    @Getter
    private TestCase lastTestCase;
    @Getter
    private String testFullName;

    private boolean rootInitialized;

    public ExecutionListener() {
        instance = this;
    }

    @Override
    @SneakyThrows
    public void executionStarted(TestIdentifier testIdentifier) {
        if (root != null) {
            if (testFullName == null) {
                UniqueId id = testIdentifier.getUniqueIdObject();
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < 3; i++) {
                    builder.append(id.getSegments().get(i).getValue().replaceAll("[^A-Za-z0-9]", "-")
                                    .replace("--", "-"))
                            .append("-");
                }
                while (builder.charAt(builder.length() - 1) == '-') builder.deleteCharAt(builder.length() - 1);
                testFullName = builder.toString();
            }
            if (!rootInitialized) {
                testExecutor.before(root);
                rootInitialized = true;
            }
            TestCase testCase = execute(testIdentifier.getUniqueIdObject(), tc -> testExecutor.before(tc));
            if (DiscoveryListener.getInstance().isLastTestCase(testIdentifier.getUniqueIdObject())) {
                lastTestCase = testCase;
            }
        }
    }

    @Override
    @SneakyThrows
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
        if (root != null) {
            if (testIdentifier.getUniqueIdObject().getLastSegment().getType().equals("test-factory")) {
                testExecutor.after(root);
                root = null;
                testExecutor = null;
                rootInitialized = false;
                testFullName = null;
                return;
            }
            execute(testIdentifier.getUniqueIdObject(), tc -> testExecutor.after(tc));
        }
    }

    private TestCase execute(UniqueId uniqueId, ConsumerWithException<TestCase> consumer) throws Exception {
        TestCase testCase = root;
        for (UniqueId.Segment segment : uniqueId.getSegments()) {
            if (segment.getType().equals("dynamic-container") || segment.getType().equals("dynamic-test")) {
                int index = Integer.parseInt(segment.getValue().substring(1)) - 1;
                if (index >= testCase.getSubTestCases().size()) {
                    return null;
                }
                testCase = testCase.getSubTestCases().get(index);
            }
        }
        consumer.accept(testCase);
        return testCase;
    }
}
