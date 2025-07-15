package io.github.dimkich.integration.testing.execution.junit;

import io.github.dimkich.integration.testing.Assertion;
import io.github.dimkich.integration.testing.TestCase;
import io.github.dimkich.integration.testing.TestCaseMapper;
import io.github.dimkich.integration.testing.execution.MockInvoke;
import io.github.dimkich.integration.testing.execution.TestExecutor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.junit.jupiter.api.function.Executable;

import java.util.List;
import java.util.Objects;

@Setter
@RequiredArgsConstructor
public class JunitExecutable implements Executable {
    private final TestExecutor testExecutor;
    private final TestCaseMapper testCaseMapper;
    private final Assertion assertion;
    private ExecutionListener executionListener;
    private TestCase rootTestCase;

    @Override
    public void execute() throws Throwable {
        TestCase testCase = rootTestCase;
        boolean isLast = false;

        for (JunitTestInfo item : executionListener.getJunitTests()) {
            if (!item.isInitialized()) {
                testExecutor.before(testCase);
                item.setInitialized(true);
            }
            isLast = isLast && testCase.isLastLeaf() || item.isLast();
            if (item.getSubTestCaseIndex() != null) {
                testCase = testCase.getSubTestCases().get(item.getSubTestCaseIndex());
            }
        }

        try {
            testExecutor.runTest(testCase);
        } finally {
            testExecutor.after(testCase);
            while ((isLast || testCase.isLastLeaf()) && testCase.getParentTestCase() != null) {
                testCase = testCase.getParentTestCase();
                testExecutor.after(testCase);
            }
            if (isLast || testCase == rootTestCase) {
                assertion.afterTests(testCaseMapper, rootTestCase);
            }
        }
    }

    public String getTestFullName() {
        return executionListener.getJunitTests().getLast().getTestFullName();
    }

    public void waitForStart() throws InterruptedException {
        testExecutor.waitForStart();
    }

    public boolean isTestRunning() {
        return testExecutor != null && testExecutor.isExecuting();
    }

    public MockInvoke search(String mockName, String method, List<Object> args) {
        return testExecutor.getTestCase().getParentsAndItselfDesc()
                .map(tc -> tc.search(mockName, method, args))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    public void addMockInvoke(MockInvoke invoke) {
        testExecutor.getTestCase().getMockInvoke().add(invoke);
    }
}
