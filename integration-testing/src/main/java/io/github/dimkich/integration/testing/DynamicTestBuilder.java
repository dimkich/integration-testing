package io.github.dimkich.integration.testing;

import io.github.dimkich.integration.testing.date.time.DateTimeService;
import io.github.dimkich.integration.testing.date.time.JavaTimeAdvice;
import io.github.dimkich.integration.testing.execution.MockAnswer;
import io.github.dimkich.integration.testing.execution.TestExecutor;
import io.github.dimkich.integration.testing.execution.junit.JunitExecutable;
import io.github.dimkich.integration.testing.execution.junit.SessionListener;
import io.github.dimkich.integration.testing.format.CompositeTestMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@RequiredArgsConstructor
public class DynamicTestBuilder {
    private final TestExecutor testExecutor;
    private final CompositeTestMapper testMapper;
    private final DateTimeService dateTimeService;
    @Value("${integration.testing.repeat:Once}")
    private String repeat;

    public Stream<DynamicNode> build(String path) throws Exception {
        return build(path, t -> true);
    }

    public Stream<DynamicNode> build(String path, List<String> allowedTestNames) throws Exception {
        return build(path, t -> {
            int i = 0;
            Iterator<Test> iterator = t.getParentsAndItselfAsc().iterator();
            while (iterator.hasNext() && i < allowedTestNames.size()) {
                Test test = iterator.next();
                if (test.getParentTest() == null) {
                    continue;
                }
                if (!allowedTestNames.get(i).equals(test.getName())) {
                    return false;
                }
                i++;
            }
            return true;
        });
    }

    public Stream<DynamicNode> build(String path, Predicate<Test> filter) throws Exception {
        testMapper.setPath(path);
        testExecutor.setExecutionListener(SessionListener.getExecutionListener());
        JavaTimeAdvice.setCallRealMethod(() -> !MockAnswer.isEnabled());
        JavaTimeAdvice.setCurrentTimeMillis(() -> dateTimeService.getDateTime().toInstant().toEpochMilli());
        JavaTimeAdvice.setGetNanoTimeAdjustment(o -> ChronoUnit.NANOS.between(Instant.ofEpochSecond(o),
                dateTimeService.getDateTime().toInstant()));
        JavaTimeAdvice.setGetDefaultRef(() -> TimeZone.getTimeZone(dateTimeService.getDateTime().getOffset()));

        Spliterator<DynamicNode> spliterator = Spliterators.spliteratorUnknownSize(
                new InfiniteTestIterator("UntilStopped".equals(repeat), filter), Spliterator.ORDERED);
        return StreamSupport.stream(spliterator, false);
    }

    @SneakyThrows
    private DynamicNode toDynamicNode(Test test, Predicate<Test> filter) {
        if (!test.getCalculatedDisabled()) {
            test.setCalculatedDisabled(!filter.test(test));
        }
        if (test.isContainer()) {
            return DynamicContainer.dynamicContainer(test.getName(), test.getSubTests().stream()
                    .map(t -> toDynamicNode(t, filter)));
        }
        return DynamicTest.dynamicTest(test.getName(), new JunitExecutable(test, testExecutor));
    }

    class InfiniteTestIterator implements Iterator<DynamicNode> {
        private final boolean infinite;
        private final Predicate<Test> filter;
        private Iterator<Test> iterator;

        public InfiniteTestIterator(boolean infinite, Predicate<Test> filter) {
            this.infinite = infinite;
            this.filter = filter;
            init();
        }

        @Override
        @SneakyThrows
        public boolean hasNext() {
            return infinite || iterator.hasNext();
        }

        @Override
        @SneakyThrows
        public DynamicNode next() {
            if (infinite && !iterator.hasNext()) {
                init();
            }
            return toDynamicNode(iterator.next(), filter);
        }

        @SneakyThrows
        private void init() {
            Test test = testMapper.readAllTests();
            iterator = test.getSubTests().iterator();
        }
    }
}
