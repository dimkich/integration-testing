package io.github.dimkich.integration.testing.execution.junit;

import io.github.dimkich.integration.testing.Assertion;
import io.github.dimkich.integration.testing.Test;
import io.github.dimkich.integration.testing.assertion.AssertionConfig;
import io.github.dimkich.integration.testing.execution.MockInvoke;
import io.github.dimkich.integration.testing.execution.TestExecutor;
import io.github.dimkich.integration.testing.format.CompositeTestMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.function.Executable;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
public class JunitExecutable implements Executable {
    private static boolean testsTempDirCleared = false;

    private final TestExecutor testExecutor;
    private final CompositeTestMapper testMapper;
    private final Assertion assertion;
    @Setter
    private ExecutionListener executionListener;
    @Setter
    private Test rootTest;
    @Getter
    private Path testsDir;

    @Override
    public void execute() throws Throwable {
        Test test = rootTest;
        boolean isLast = false;

        for (JunitTestInfo item : executionListener.getJunitTests()) {
            if (item.getIndex() != null) {
                test = test.getSubTests().get(item.getIndex());
            }
            if (!item.isInitialized()) {
                testExecutor.before(test);
                item.setInitialized(true);
                if (test == rootTest) {
                    testsDir = null;
                    if (assertion.useTestTempDir()) {
                        testsDir = Path.of(AssertionConfig.resultDir + File.separator + item.getTestFilePath());
                        if (!testsTempDirCleared) {
                            FileSystemUtils.deleteRecursively(testsDir);
                            testsTempDirCleared = true;
                        }
                    }
                }
            }
            isLast = isLast && test.isLastLeaf() || item.isLast();
        }

        boolean disabled = testExecutor.getTest() == null;
        try {
            testExecutor.runTest();
        } finally {
            testExecutor.after(test);
            while ((isLast || test.isLastLeaf()) && test.getParentTest() != null) {
                test = test.getParentTest();
                testExecutor.after(test);
            }
            if (isLast || test == rootTest) {
                assertion.afterTests(testMapper, rootTest);
            }
            if (disabled) {
                Assumptions.abort();
            }
        }
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
