package io.github.dimkich.integration.testing;

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
    private final JunitExecutable junitExecutable;
    private final TestCaseMapper testCaseMapper;

    public Stream<DynamicNode> build(String path) throws Exception {
        testCaseMapper.setPath(path);
        TestCase testCase = testCaseMapper.readAllTestCases();
        junitExecutable.setRootTestCase(testCase);
        junitExecutable.setExecutionListener(ExecutionListener.getLast());
        return testCase.getSubTestCases().stream().map(this::toDynamicNode);
    }

    @SneakyThrows
    private DynamicNode toDynamicNode(TestCase testCase) {
        if (testCase.isContainer()) {
            return DynamicContainer.dynamicContainer(testCase.getName(), testCase.getSubTestCases().stream()
                    .map(this::toDynamicNode));
        }
        return DynamicTest.dynamicTest(testCase.getName(), junitExecutable);
    }
}
