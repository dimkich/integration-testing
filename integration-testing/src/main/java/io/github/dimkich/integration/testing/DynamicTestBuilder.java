package io.github.dimkich.integration.testing;

import io.github.dimkich.integration.testing.execution.TestExecutor;
import io.github.dimkich.integration.testing.execution.junit.ExecutionListener;
import io.github.dimkich.integration.testing.execution.junit.JunitExecutable;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;

import java.util.stream.Stream;

@RequiredArgsConstructor
public class DynamicTestBuilder {
    private final TestExecutor testExecutor;
    private final JunitExecutable junitExecutable;
    private final TestCaseMapper testCaseMapper;

    public Stream<DynamicNode> build(String path) throws Exception {
        testCaseMapper.setPath(path);
        TestCase testCase = testCaseMapper.readAllTestCases();
        ExecutionListener.getInstance().setRoot(testCase);
        ExecutionListener.getInstance().setTestExecutor(testExecutor);
        return testCase.getSubTestCases().stream().map(this::toDynamicNode);
    }

    @SneakyThrows
    private DynamicNode toDynamicNode(TestCase testCase) {
        if (testCase.isContainer()) {
            return DynamicContainer.dynamicContainer(testCase.getName(), testCase.getSubTestCases().stream()
                    .map(this::toDynamicNode));
        }
        junitExecutable.setTestCase(testCase);
        return DynamicTest.dynamicTest(testCase.getName(), junitExecutable);
    }
}
