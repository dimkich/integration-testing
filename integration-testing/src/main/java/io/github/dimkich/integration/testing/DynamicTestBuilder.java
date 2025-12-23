package io.github.dimkich.integration.testing;

import io.github.dimkich.integration.testing.date.time.DateTimeService;
import io.github.dimkich.integration.testing.date.time.JavaTimeAdvice;
import io.github.dimkich.integration.testing.date.time.MockJavaTimeSetUp;
import io.github.dimkich.integration.testing.execution.MockAnswer;
import io.github.dimkich.integration.testing.execution.TestExecutor;
import io.github.dimkich.integration.testing.execution.junit.JunitExecutable;
import io.github.dimkich.integration.testing.execution.junit.SessionListener;
import io.github.dimkich.integration.testing.format.CompositeTestMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
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

/**
 * Builds a JUnit 5 dynamic test tree from integration test descriptions.
 * <p>
 * The builder delegates to {@link CompositeTestMapper} to read tests from a path
 * and to {@link TestExecutor} to execute individual tests. It also configures
 * time-related behavior when {@link MockJavaTimeSetUp} is initialized and
 * supports repeated execution controlled by the {@code integration.testing.repeat}
 * property (e.g. {@code Once}, {@code UntilStopped}).
 */
@Slf4j
@RequiredArgsConstructor
public class DynamicTestBuilder {
    private final TestExecutor testExecutor;
    private final CompositeTestMapper testMapper;
    private final DateTimeService dateTimeService;
    @Value("${integration.testing.repeat:Once}")
    private String repeat;

    /**
     * Builds a stream of dynamic test nodes from the given path without any additional filtering.
     *
     * @param path path to the test description resource (for example, a file or classpath location)
     * @return stream of {@link DynamicNode} representing the root tests and containers
     * @throws Exception if reading or mapping tests fails
     */
    public Stream<DynamicNode> build(String path) throws Exception {
        return build(path, t -> true);
    }

    /**
     * Builds a stream of dynamic test nodes from the given path and filters the tree
     * by a list of allowed test names along the parent chain.
     * <p>
     * Only tests whose ancestor names match the {@code allowedTestNames} sequence (from
     * top parent to child) are included.
     *
     * @param path             path to the test description resource
     * @param allowedTestNames ordered list of test names that must match parents and the test itself
     * @return stream of {@link DynamicNode} that satisfy the name filter
     * @throws Exception if reading or mapping tests fails
     */
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

    /**
     * Builds a stream of dynamic test nodes from the given path using the provided filter.
     * <p>
     * The test tree is read once per iteration cycle and wrapped in an {@link InfiniteTestIterator}
     * that can either terminate after a single pass or reinitialize and continue indefinitely
     * depending on the {@code integration.testing.repeat} property.
     *
     * @param path   path to the test description resource
     * @param filter predicate used to enable or disable tests dynamically
     * @return stream of {@link DynamicNode} backed by an {@link InfiniteTestIterator}
     * @throws Exception if reading or mapping tests fails
     */
    public Stream<DynamicNode> build(String path, Predicate<Test> filter) throws Exception {
        testMapper.setPath(path);
        testExecutor.setExecutionListener(SessionListener.getExecutionListener());
        if (MockJavaTimeSetUp.isInitialized()) {
            JavaTimeAdvice.setCallRealMethod(() -> !MockAnswer.isEnabled());
            JavaTimeAdvice.setCurrentTimeMillis(() -> dateTimeService.getDateTime().toInstant().toEpochMilli());
            JavaTimeAdvice.setGetNanoTimeAdjustment(o -> ChronoUnit.NANOS.between(Instant.ofEpochSecond(o),
                    dateTimeService.getDateTime().toInstant()));
            JavaTimeAdvice.setGetDefaultRef(() -> TimeZone.getTimeZone(dateTimeService.getDateTime().getOffset()));
        }

        Spliterator<DynamicNode> spliterator = Spliterators.spliteratorUnknownSize(
                new InfiniteTestIterator("UntilStopped".equals(repeat), filter), Spliterator.ORDERED);
        return StreamSupport.stream(spliterator, false);
    }

    /**
     * Converts a {@link Test} description into a corresponding JUnit {@link DynamicNode}.
     * <p>
     * Containers are mapped to {@link DynamicContainer} and leaf tests to {@link DynamicTest}.
     *
     * @param test   integration test description
     * @param filter predicate used to determine whether the test should be disabled
     * @return a dynamic container or test node
     */
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

    /**
     * Iterator that yields dynamic nodes over the current test tree and can optionally
     * restart from the beginning when the end is reached.
     */
    class InfiniteTestIterator implements Iterator<DynamicNode> {
        private final boolean infinite;
        private final Predicate<Test> filter;
        private Iterator<Test> iterator;

        /**
         * Creates a new iterator.
         *
         * @param infinite if {@code true}, iteration restarts from the beginning when exhausted;
         *                 otherwise iteration ends once all tests have been visited
         * @param filter   predicate used to filter tests when converting to {@link DynamicNode}
         */
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
            while (infinite && !iterator.hasNext()) {
                init();
            }
            return toDynamicNode(iterator.next(), filter);
        }

        /**
         * (Re)initializes the underlying iterator by reading the full test tree
         * and setting the iterator to its immediate subtests.
         */
        @SneakyThrows
        private void init() {
            try {
                Test test = testMapper.readAllTests();
                iterator = test.getSubTests().iterator();
            } catch (Exception e) {
                if (!infinite) {
                    throw e;
                }
                log.error("", e);
                iterator = Collections.emptyIterator();
            }
        }
    }
}
