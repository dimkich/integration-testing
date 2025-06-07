package io.github.dimkich.integration.testing.execution.junit;

import io.github.dimkich.integration.testing.Assertion;
import io.github.dimkich.integration.testing.TestCase;
import io.github.dimkich.integration.testing.TestCaseMapper;
import io.github.dimkich.integration.testing.execution.MockInvoke;
import io.github.dimkich.integration.testing.execution.TestExecutor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.junit.jupiter.api.function.Executable;

import java.util.List;
import java.util.Objects;

@Getter
@Setter
@RequiredArgsConstructor
public class JunitExecutable implements Executable {
    private final TestExecutor testExecutor;
    private final Assertion assertion;
    private final TestCaseMapper testCaseMapper;
    private TestCase testCase;

    @Override
    public void execute() throws Throwable {
        try {
            testExecutor.runTest(testCase);
        } finally {
            if (testCase.isLast()) {
                assertion.afterTests(testCaseMapper, testCase.getRootTestCase());
            }
        }
    }

    public void waitForStart() throws InterruptedException {
        testExecutor.waitForStart();
    }

    public boolean isTestRunning() {
        return testExecutor != null && testExecutor.getTestCase() != null;
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
