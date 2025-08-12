package io.github.dimkich.integration.testing;

import io.github.dimkich.integration.testing.date.time.DateTimeService;
import io.github.dimkich.integration.testing.date.time.JavaTimeAdvice;
import io.github.dimkich.integration.testing.execution.TestExecutor;
import io.github.dimkich.integration.testing.execution.junit.ExecutionListener;
import io.github.dimkich.integration.testing.execution.junit.JunitExecutable;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.TimeZone;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class DynamicTestBuilder {
    private final JunitExecutable junitExecutable;
    private final TestMapper testMapper;
    private final TestExecutor testExecutor;
    private final DateTimeService dateTimeService;

    public Stream<DynamicNode> build(String path) throws Exception {
        testMapper.setPath(path);
        Test test = testMapper.readAllTests();
        test.check();
        junitExecutable.setRootTest(test);
        junitExecutable.setExecutionListener(ExecutionListener.getLast());
        JavaTimeAdvice.setCallRealMethod(() -> !testExecutor.isExecuting());
        JavaTimeAdvice.setCurrentTimeMillis(() -> dateTimeService.getDateTime().toInstant().toEpochMilli());
        JavaTimeAdvice.setGetNanoTimeAdjustment(o -> ChronoUnit.NANOS.between(Instant.ofEpochSecond(o),
                dateTimeService.getDateTime().toInstant()));
        JavaTimeAdvice.setGetDefaultRef(() -> TimeZone.getTimeZone(dateTimeService.getDateTime().getOffset()));
        return test.getSubTests().stream().map(this::toDynamicNode);
    }

    @SneakyThrows
    private DynamicNode toDynamicNode(Test test) {
        test.check();
        if (test.isContainer()) {
            return DynamicContainer.dynamicContainer(test.getName(), test.getSubTests().stream()
                    .map(this::toDynamicNode));
        }
        return DynamicTest.dynamicTest(test.getName(), junitExecutable);
    }
}
