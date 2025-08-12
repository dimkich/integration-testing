package io.github.dimkich.integration.testing.initialization;

import io.github.dimkich.integration.testing.Test;
import io.github.dimkich.integration.testing.TestInit;
import lombok.RequiredArgsConstructor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class InitState {
    private final TestInit.Initializer<TestInit> initializer;
    private final Deque<TestInit> testDefaults = new ArrayDeque<>();
    private final Deque<TestInit> testContainers = new ArrayDeque<>();
    private final Deque<TestInit> testCases = new ArrayDeque<>();
    private final Deque<TestInit> testParts = new ArrayDeque<>();

    public void add(TestInit init) {
        if (init.getApplyTo() == null) {
            testDefaults.add(init);
            return;
        }
        switch (init.getApplyTo()) {
            case TestContainer -> testContainers.add(init);
            case TestCase -> testCases.add(init);
            case TestPart -> testParts.add(init);
            case All -> {
                testContainers.add(init);
                testCases.add(init);
                testParts.add(init);
            }
        }
    }

    public void remove(TestInit init) {
        if (init.getApplyTo() == null) {
            return;
        }
        switch (init.getApplyTo()) {
            case TestContainer -> {
                assert init.equals(testContainers.removeLast());
            }
            case TestCase -> {
                assert init.equals(testCases.removeLast());
            }
            case TestPart -> {
                assert init.equals(testParts.removeLast());
            }
            case All -> {
                assert init.equals(testContainers.removeLast());
                assert init.equals(testCases.removeLast());
                assert init.equals(testParts.removeLast());
            }
        }
    }

    public void init(Test.Type type) throws Exception {
        Deque<TestInit> tests = switch (type) {
            case TestContainer -> testContainers;
            case TestCase -> testCases;
            case TestPart -> testParts;
        };
        if (!tests.isEmpty() || !testDefaults.isEmpty()) {
            initializer.init(Stream.concat(tests.stream(), testDefaults.stream()));
        }
        testDefaults.clear();
    }
}
