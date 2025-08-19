package io.github.dimkich.integration.testing.execution.junit;

import io.github.dimkich.integration.testing.Assertion;
import io.github.dimkich.integration.testing.Test;
import io.github.dimkich.integration.testing.execution.MockInvoke;
import io.github.dimkich.integration.testing.execution.TestExecutor;
import io.github.dimkich.integration.testing.format.CompositeTestMapper;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.junit.jupiter.api.function.Executable;

import java.util.List;
import java.util.Objects;

@Setter
@RequiredArgsConstructor
public class JunitExecutable implements Executable {
    private final TestExecutor testExecutor;
    private final CompositeTestMapper testMapper;
    private final Assertion assertion;
    private ExecutionListener executionListener;
    private Test rootTest;

    @Override
    public void execute() throws Throwable {
        Test test = rootTest;
        boolean isLast = false;

        for (JunitTestInfo item : executionListener.getJunitTests()) {
            if (!item.isInitialized()) {
                testExecutor.before(test);
                item.setInitialized(true);
            }
            isLast = isLast && test.isLastLeaf() || item.isLast();
            if (item.getSubTestIndex() != null) {
                test = test.getSubTests().get(item.getSubTestIndex());
            }
        }

        try {
            testExecutor.runTest(test);
        } finally {
            testExecutor.after(test);
            while ((isLast || test.isLastLeaf()) && test.getParentTest() != null) {
                test = test.getParentTest();
                testExecutor.after(test);
            }
            if (isLast || test == rootTest) {
                assertion.afterTests(testMapper, rootTest);
            }
        }
    }

    public String getTestFullName() {
        return executionListener.getJunitTests().getLast().getTestFullName();
    }

    public boolean isTestRunning() {
        return testExecutor != null && testExecutor.isExecuting();
    }

    public MockInvoke search(String mockName, String method, List<Object> args) {
        return testExecutor.getTest().getParentsAndItselfDesc()
                .map(tc -> tc.search(mockName, method, args))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    public void addMockInvoke(MockInvoke invoke) {
        testExecutor.getTest().getMockInvoke().add(invoke);
    }
}
