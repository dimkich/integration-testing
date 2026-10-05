package io.github.dimkich.integration.testing.execution;

import io.github.dimkich.integration.testing.*;
import io.github.dimkich.integration.testing.assertion.AssertionConfig;
import io.github.dimkich.integration.testing.execution.junit.ExecutionListener;
import io.github.dimkich.integration.testing.format.CompositeTestMapper;
import io.github.dimkich.integration.testing.initialization.InitializationService;
import io.github.dimkich.integration.testing.message.AbstractMessage;
import io.github.dimkich.integration.testing.message.TestMessages;
import io.github.dimkich.integration.testing.storage.TestDataStorages;
import io.github.dimkich.integration.testing.wait.completion.WaitCompletionList;
import io.github.sugarcubes.cloner.Cloner;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assumptions;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

/**
 * Executes integration tests with support for lifecycle management, message handling,
 * and test assertions.
 * <p>
 * This class orchestrates the complete test execution lifecycle:
 * <ul>
 *   <li><b>Before phase:</b> Initializes test environment, sets up test data storages,
 *       executes before-test hooks, and prepares test directories</li>
 *   <li><b>Execution phase:</b> Runs the test method or sends inbound messages,
 *       polls for outbound messages, tracks data storage changes, and performs assertions</li>
 *   <li><b>After phase:</b> Executes after-test hooks, performs cleanup, and handles
 *       test completion</li>
 * </ul>
 * <p>
 * The executor supports both method-based test execution and message-driven test execution.
 * It integrates with Spring's BeanFactory for dependency injection and uses various
 * extension points (BeforeTest, AfterTest, TestConverter) for customization.
 * <p>
 * Thread safety: This class is not thread-safe. Each test execution should use
 * a separate instance or ensure proper synchronization.
 *
 * @author dimkich
 */
@RequiredArgsConstructor
@Slf4j
@Setter
public class TestExecutor {
    /**
     * Flag to ensure the tests temporary directory is cleared only once per test run.
     */
    private static boolean testsTempDirCleared = false;
    /**
     * Spring BeanFactory for accessing and instantiating beans during test execution.
     */
    private final BeanFactory beanFactory;
    /**
     * Assertion service for comparing expected and actual test results.
     */
    private final Assertion assertion;
    /**
     * Manages waiting for asynchronous operations to complete during test execution.
     */
    private final WaitCompletionList waitCompletion;
    /**
     * List of before-test hooks to execute before each test.
     */
    private final List<BeforeTest> beforeTests;
    /**
     * List of test converters to transform test data during execution.
     */
    private final List<TestConverter> testConverters;
    /**
     * List of after-test hooks to execute after each test.
     */
    private final List<AfterTest> afterTests;
    /**
     * Mapper for formatting and mapping test information.
     */
    private final CompositeTestMapper testMapper;
    /**
     * Cloner for deep copying test results to avoid side effects.
     */
    private final Cloner cloner;
    /**
     * Message store for sending inbound messages and retrieving outbound messages
     * during test execution.
     */
    private final TestMessages testMessages;
    /**
     * Service for initializing and cleaning up test state before and after tests.
     */
    @Setter(onMethod_ = {@Autowired, @Lazy})
    private InitializationService initializationService;

    /**
     * When true, errors during inbound message sending are captured in the test response
     * instead of propagating. This enables testing error scenarios like adapter chain
     * failures. Intended for framework self-testing only.
     */
    @Setter(onMethod_ = @Value("${integration.testing.self-testing:false}"))
    private boolean selfTesting;
    /**
     * Manages test data storages and tracks differences between expected and actual states.
     */
    @Setter(onMethod_ = {@Autowired, @Lazy})
    private TestDataStorages testDataStorages;
    /**
     * Directory path where test temporary files are stored.
     * Only set for root-level tests when test temporary directory is enabled.
     */
    @Getter
    private Path testsDir;
    /**
     * The currently executing test instance.
     */
    @Getter
    private Test test;
    /**
     * Listener for test execution events, used to track test hierarchy and find previous tests.
     */
    @Setter
    private ExecutionListener executionListener;
    /**
     * The last executed test in the test hierarchy, used for context in after hooks.
     */
    @Getter
    @Setter
    private Test lastTest;
    /**
     * Expected outbound messages of the currently executed test, saved before the
     * {@link Test} instance is cleared. Used to align the captured messages to the
     * order defined in the test file.
     */
    private List<AbstractMessage> expectedOutboundMessages;

    /**
     * Prepares the test for execution by initializing the test state and executing
     * before-test hooks.
     * <p>
     * This method:
     * <ul>
     *   <li>Sets the current test instance</li>
     *   <li>Logs test information</li>
     *   <li>Skips disabled tests</li>
     *   <li>Resets test state (response, data storage diff, outbound messages)</li>
     *   <li>Executes test's before hooks and before-test extensions</li>
     *   <li>Initializes test data storages if available</li>
     * </ul>
     * <p>
     * For root-level tests, this method also:
     * <ul>
     *   <li>Finds the last executed test</li>
     *   <li>Creates and clears the test temporary directory if enabled</li>
     *   <li>Affects all test data storages</li>
     * </ul>
     *
     * @param expectedTest the test to prepare for execution
     * @throws Exception if an error occurs during test preparation
     */
    public void before(Test expectedTest) throws Exception {
        expectedOutboundMessages = null;
        test = expectedTest;
        log.info(">>> {}", test.getFullName());
        log.info(testMapper.getCurrentPathAndLocation(test));
        if (test.getCalculatedDisabled()) {
            return;
        }
        expectedOutboundMessages = test.getOutboundMessages() == null
                ? null : new ArrayList<>(test.getOutboundMessages());
        assertion.setExpected(test);
        test.setResponse(null);
        test.setDataStorageDiff(null);
        test.setOutboundMessages(null);
        testMessages.resetInbound();
        this.test.before(this::beforeConsumer, this::afterConsumer);
        if (testDataStorages != null) {
            testDataStorages.setNewCurrentValue();
        }
    }

    /**
     * Internal callback executed during the before phase of test execution.
     * <p>
     * Handles root-level test initialization including directory setup and
     * test data storage initialization. Also executes initialization service
     * and before-test hooks.
     *
     * @param t the test being prepared
     * @throws Exception if an error occurs during initialization
     */
    private void beforeConsumer(Test t) throws Exception {
        if (t.getParentTest() == null) {
            lastTest = executionListener.findLastTest(t);
            testsDir = null;
            if (assertion.useTestTempDir()) {
                testsDir = Path.of(AssertionConfig.resultDir).resolve(executionListener.getTestFilePath());
                if (!testsTempDirCleared) {
                    File dir = new File(AssertionConfig.resultDir);
                    if (!dir.exists() && !dir.mkdirs()) {
                        throw new RuntimeException("Failed to create directory " + dir.getAbsolutePath());
                    }
                    for (File file : Objects.requireNonNull(dir.listFiles())) {
                        FileSystemUtils.deleteRecursively(file);
                    }
                    testsTempDirCleared = true;
                }
            }
            if (testDataStorages != null) {
                testDataStorages.affectAllStorages();
            }
        }
        t.clearCustom();
        initializationService.beforeTest(t);
        for (BeforeTest beforeTest : beforeTests) {
            beforeTest.before(t);
        }
    }

    /**
     * Executes the test by either running the test method or sending an inbound message.
     * <p>
     * The execution flow:
     * <ol>
     *   <li>Starts the wait completion tracker for asynchronous operations</li>
     *   <li>If the test has an inbound message, sends it through the message store</li>
     *   <li>Otherwise, executes the test method using reflection on Spring beans</li>
     *   <li>Waits for all asynchronous operations to complete</li>
     *   <li>Polls for outbound messages if a message store is configured</li>
     *   <li>Captures data storage differences if test data storages are configured</li>
     *   <li>Applies all test converters to transform the test data</li>
     *   <li>Performs assertions comparing expected and actual test results</li>
     * </ol>
     * <p>
     *
     * @throws Exception if an error occurs during test execution or assertion
     */
    public void runTest() throws Exception {
        if (test.getCalculatedDisabled()) {
            return;
        }
        waitCompletion.start();
        MockAnswer.enable(() -> {
            try {
                AbstractMessage message = test.getInboundMessage();
                if (message != null) {
                    try {
                        testMessages.send(message);
                    } catch (Exception e) {
                        log.error("", e);
                        if (selfTesting) {
                            test.setResponse(e);
                        } else {
                            throw e;
                        }
                    }
                } else {
                    test.executeMethod(beanFactory, (m, r) -> cloner.clone(r));
                }
            } finally {
                waitCompletion.waitCompletion();
            }
        });

        List<AbstractMessage> messages = testMessages.pollMessages();
        test.setOutboundMessages(messages.isEmpty() ? null : messages);
        if (testDataStorages != null) {
            test.setDataStorageDiff(testDataStorages.getMapDiff());
        }
        testConverters.forEach(c -> c.convertNoException(test));
        if (test.getOutboundMessages() != null) {
            test.setOutboundMessages(alignActualToExpected(expectedOutboundMessages, test.getOutboundMessages()));
        }

        assertion.assertTestsEquals(test);
    }

    /**
     * Cleans up after test execution by executing after-test hooks and performing
     * final cleanup operations.
     * <p>
     * This method:
     * <ul>
     *   <li>Executes the test's after hooks and after-test extensions</li>
     *   <li>Aborts the test if it was disabled</li>
     *   <li>Clears the current test reference</li>
     * </ul>
     * <p>
     * The test reference is always cleared, even if an exception occurs during the hooks.
     *
     * @throws Exception if an error occurs during cleanup
     */
    public void after() throws Exception {
        try {
            test.after(this::afterConsumer, lastTest);
            if (test.getCalculatedDisabled()) {
                Assumptions.abort();
            }
        } finally {
            this.test = null;
        }
    }

    /**
     * Internal callback executed during the after phase of test execution.
     * <p>
     * Executes the initialization service cleanup, after-test hooks, and
     * final assertions for root-level tests.
     *
     * @param t the test being cleaned up
     * @throws Exception if an error occurs during cleanup
     */
    private void afterConsumer(Test t) throws Exception {
        initializationService.afterTest(t);
        for (AfterTest afterTest : afterTests) {
            afterTest.after(t);
        }
        if (t.getParentTest() == null) {
            assertion.afterTests(t);
        }
    }

    /**
     * Searches for a mock invocation matching the specified criteria in the test hierarchy.
     * <p>
     * The search traverses the test hierarchy from the current test up to the root,
     * looking for a MockInvoke that matches the given mock name, method name, and arguments.
     * The first matching mock invocation found is returned.
     *
     * @param mockName the name of the mock to search for
     * @param method   the method name to match
     * @param args     the list of arguments to match against the mock invocation
     * @return the matching MockInvoke if found, or null if no match is found
     */
    public MockInvoke search(String mockName, String method, List<Object> args) {
        return test.getParentsAndItselfDesc()
                .map(tc -> tc.search(mockName, method, args))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /**
     * Adds a mock invocation to the current test's mock invocation list.
     * <p>
     * Mock invocations are used to track and verify method calls on mocked objects
     * during test execution.
     *
     * @param invoke the mock invocation to add to the current test
     */
    public void addMockInvoke(MockInvoke invoke) {
        test.getMockInvoke().add(invoke);
    }

    /**
     * Aligns actual messages to the order defined in expected messages using multiset matching.
     * Unmatched actual messages are appended at the end in a deterministic (canonical) order.
     *
     * @param expectedMessages expected messages in the order they are defined in the test file
     * @param actualMessages   captured messages in their arrival order
     * @return actual messages aligned to the expected order
     * @throws Exception if a message cannot be serialized to a canonical form
     */
    private List<AbstractMessage> alignActualToExpected(List<AbstractMessage> expectedMessages,
                                                        List<AbstractMessage> actualMessages) throws Exception {
        if (expectedMessages == null || expectedMessages.isEmpty()) {
            return canonicalSort(actualMessages);
        }
        List<AbstractMessage> aligned = new ArrayList<>();
        List<AbstractMessage> remainingActual = new LinkedList<>(actualMessages);
        for (AbstractMessage expected : expectedMessages) {
            String expectedKey = getMessageKey(expected);
            Iterator<AbstractMessage> iterator = remainingActual.iterator();
            while (iterator.hasNext()) {
                AbstractMessage actual = iterator.next();
                if (expectedKey.equals(getMessageKey(actual))) {
                    aligned.add(actual);
                    iterator.remove();
                    break;
                }
            }
        }
        aligned.addAll(canonicalSort(remainingActual));
        return aligned;
    }

    /**
     * Sorts messages by their canonical serialized representation. Keys are computed
     * beforehand to keep the sorting itself free of checked exceptions and to avoid
     * repeated serialization of the same message.
     *
     * @param messages messages to sort
     * @return messages sorted by their canonical representation
     * @throws Exception if a message cannot be serialized to a canonical form
     */
    private List<AbstractMessage> canonicalSort(List<AbstractMessage> messages) throws Exception {
        if (messages == null || messages.isEmpty()) {
            return messages;
        }
        List<Map.Entry<String, AbstractMessage>> keyedMessages = new ArrayList<>(messages.size());
        for (AbstractMessage message : messages) {
            keyedMessages.add(new AbstractMap.SimpleEntry<>(getMessageKey(message), message));
        }
        keyedMessages.sort(Map.Entry.comparingByKey());
        List<AbstractMessage> sorted = new ArrayList<>(messages.size());
        for (Map.Entry<String, AbstractMessage> entry : keyedMessages) {
            sorted.add(entry.getValue());
        }
        return sorted;
    }

    /**
     * Builds a canonical string key of a message exactly as it is seen by the assertion:
     * the message is wrapped into a dummy test part and serialized by the current test mapper.
     *
     * @param message message to build the key for
     * @return canonical string representation of the message
     * @throws Exception if the message cannot be serialized
     */
    private String getMessageKey(AbstractMessage message) throws Exception {
        TestPart wrapper = new TestPart();
        wrapper.setOutboundMessages(List.of(message));
        return testMapper.getSingleTestAsString(wrapper);
    }
}
