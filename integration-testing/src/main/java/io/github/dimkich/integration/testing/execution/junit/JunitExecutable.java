package io.github.dimkich.integration.testing.execution.junit;

import io.github.dimkich.integration.testing.Test;
import io.github.dimkich.integration.testing.execution.TestExecutor;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.function.Executable;

@RequiredArgsConstructor
public class JunitExecutable implements Executable {
    private final Test test;
    private final TestExecutor testExecutor;

    @Override
    public void execute() throws Throwable {
        testExecutor.before(test);
        try {
            testExecutor.runTest();
        } finally {
            testExecutor.after();
        }
    }
}
