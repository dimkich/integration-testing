package io.github.dimkich.integration.testing.execution;

import io.github.dimkich.integration.testing.*;
import io.github.dimkich.integration.testing.execution.junit.JunitExtension;
import io.github.dimkich.integration.testing.initialization.InitializationService;
import io.github.dimkich.integration.testing.message.MessageDto;
import io.github.dimkich.integration.testing.message.TestMessagePoller;
import io.github.dimkich.integration.testing.message.TestMessageSender;
import io.github.dimkich.integration.testing.storage.TestDataStorages;
import io.github.dimkich.integration.testing.wait.completion.WaitCompletionList;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import java.util.Comparator;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Setter
public class TestExecutor {
    private volatile boolean running = false;
    private final BeanFactory beanFactory;
    private final Assertion assertion;
    private final WaitCompletionList waitCompletion;
    private final List<BeforeTestCase> beforeTestCases;
    private final List<TestCaseConverter> testCaseConverters;
    private final List<AfterTestCase> afterTestCases;
    private final List<TestMessageSender> testMessageSenders;
    private final TestCaseMapper testCaseMapper;
    @Setter(onMethod_ = {@Autowired, @Lazy})
    private InitializationService initializationService;
    @Setter(onMethod_ = {@Autowired, @Lazy})
    private TestDataStorages testDataStorages;
    @Setter(onMethod_ = @Autowired(required = false))
    private TestMessagePoller testMessagePoller;
    @Getter
    private TestCase testCase;

    public void waitForStart() throws InterruptedException {
        if (running) {
            return;
        }
        if (JunitExtension.isRunningTestThread()) {
            return;
        }
        synchronized (this) {
            if (running) {
                return;
            }
            wait();
        }
    }

    public void before(TestCase testCase) throws Exception {
        testCase.getParentsAndItselfAsc()
                .flatMap(tc -> tc.getInits().stream())
                .filter(i -> i.getActualLevel() == testCase.getLevel())
                .forEach(initializationService::init);
        for (BeforeTestCase beforeTestCase : beforeTestCases) {
            beforeTestCase.accept(testCase);
        }
    }

    public void runTest(TestCase expectedTestCase) throws Exception {
        if (!running) {
            synchronized (this) {
                running = true;
                notifyAll();
            }
        }
        testCase = expectedTestCase;
        log.info(">>> {}", testCase.getFullName());
        log.info(testCaseMapper.getCurrentPathAndLocation(testCase).replace("\\", "/"));
        if (assertion.makeTestCaseDeepClone()) {
            testCase = testCaseMapper.deepClone(testCase);
        }

        try {
            waitCompletion.start();
            MessageDto<?> message = testCase.getInboundMessage();
            if (message != null) {
                testMessageSenders.stream()
                        .filter(s -> s.canSend(message))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("No service found for message " + message))
                        .sendInboundMessage(message);
            } else {
                testCase.executeMethod(beanFactory, (m, r) -> testCaseMapper.deepClone(r));
            }
        } finally {
            waitCompletion.waitCompletion();
        }

        int countMessages = testCase.getOutboundMessages() == null ? 0 : testCase.getOutboundMessages().size();
        testCase.setOutboundMessages(null);
        if (testMessagePoller != null) {
            List<MessageDto<?>> messages = testMessagePoller.pollMessages(countMessages);
            messages.sort(Comparator.comparing(MessageDto::toString));
            testCase.setOutboundMessages(messages.isEmpty() ? null : messages);
        }
        testCase.setDataStorageDiff(null);
        if (testDataStorages != null) {
            testCase.setDataStorageDiff(testDataStorages.getMapDiff());
        }
        testCaseConverters.forEach(c -> c.convertNoException(testCase));

        assertion.assertTestCaseEquals(testCaseMapper, expectedTestCase, testCase);
    }

    public void after(TestCase testCase) throws Exception {
        this.testCase = null;
        for (AfterTestCase afterTestCase : afterTestCases) {
            afterTestCase.accept(testCase);
        }
    }
}
