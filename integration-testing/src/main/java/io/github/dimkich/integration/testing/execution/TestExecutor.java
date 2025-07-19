package io.github.dimkich.integration.testing.execution;

import eu.ciechanowiec.sneakyfun.SneakyConsumer;
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
    private TestCase expectedTestCase;
    @Getter
    @Setter
    private boolean executing = false;

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

    public void before(TestCase expectedTestCase) throws Exception {
        this.expectedTestCase = expectedTestCase;
        if (assertion.makeTestCaseDeepClone()) {
            testCase = testCaseMapper.deepClone(expectedTestCase);
        } else {
            testCase = expectedTestCase;
            testCase.setResponse(null);
            testCase.setDataStorageDiff(null);
            testCase.setOutboundMessages(null);
        }
        if (!running) {
            synchronized (this) {
                running = true;
                notifyAll();
            }
        }
        testCase.getParentsAndItselfAsc()
                .flatMap(tc -> tc.getInits().stream())
                .filter(i -> i.getActualLevel() == testCase.getLevel())
                .sorted(Comparator.comparing(TestCaseInit::getOrder))
                .forEach(SneakyConsumer.sneaky(initializationService::init));
        for (BeforeTestCase beforeTestCase : beforeTestCases) {
            beforeTestCase.accept(testCase);
        }
        if (testDataStorages != null) {
            testDataStorages.setNewCurrentValue();
        }
    }

    public void runTest(TestCase tc) throws Exception {
        log.info(">>> {}", testCase.getFullName());
        log.info(testCaseMapper.getCurrentPathAndLocation(testCase));
        waitCompletion.start();
        executing = true;
        try {
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
            try {
                waitCompletion.waitCompletion();
            } finally {
                executing = false;
            }
        }

        int countMessages = testCase.getOutboundMessages() == null ? 0 : testCase.getOutboundMessages().size();
        if (testMessagePoller != null) {
            List<MessageDto<?>> messages = testMessagePoller.pollMessages(countMessages);
            messages.sort(Comparator.comparing(MessageDto::toString));
            testCase.setOutboundMessages(messages.isEmpty() ? null : messages);
        }
        if (testDataStorages != null) {
            testCase.setDataStorageDiff(testDataStorages.getMapDiff());
        }
        testCaseConverters.forEach(c -> c.convertNoException(testCase));

        assertion.assertTestCaseEquals(testCaseMapper, this.expectedTestCase, testCase);
    }

    public void after(TestCase testCase) throws Exception {
        try {
            for (AfterTestCase afterTestCase : afterTestCases) {
                afterTestCase.accept(testCase);
            }
        } finally {
            this.testCase = null;
            this.expectedTestCase = null;
        }
    }
}
