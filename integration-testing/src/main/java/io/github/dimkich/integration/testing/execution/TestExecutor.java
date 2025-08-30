package io.github.dimkich.integration.testing.execution;

import io.github.dimkich.integration.testing.*;
import io.github.dimkich.integration.testing.format.CompositeTestMapper;
import io.github.dimkich.integration.testing.initialization.InitializationService;
import io.github.dimkich.integration.testing.message.MessageDto;
import io.github.dimkich.integration.testing.message.TestMessagePoller;
import io.github.dimkich.integration.testing.message.TestMessageSender;
import io.github.dimkich.integration.testing.storage.TestDataStorages;
import io.github.dimkich.integration.testing.wait.completion.WaitCompletionList;
import io.github.sugarcubes.cloner.Cloner;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Setter
public class TestExecutor {
    private final BeanFactory beanFactory;
    private final Assertion assertion;
    private final WaitCompletionList waitCompletion;
    private final List<BeforeTest> beforeTests;
    private final List<TestConverter> testConverters;
    private final List<AfterTest> afterTests;
    private final List<TestMessageSender> testMessageSenders;
    private final CompositeTestMapper testMapper;
    private final Cloner cloner;
    @Setter(onMethod_ = {@Autowired, @Lazy})
    private InitializationService initializationService;
    @Setter(onMethod_ = {@Autowired, @Lazy})
    private TestDataStorages testDataStorages;
    @Setter(onMethod_ = @Autowired(required = false))
    private TestMessagePoller testMessagePoller;

    private final Deque<Test> testsToSkip = new ArrayDeque<>();
    @Getter
    private Test test;
    private Test expectedTest;

    public void before(Test expectedTest) throws Exception {
        log.info(">>> {}", expectedTest.getFullName());
        log.info(testMapper.getCurrentPathAndLocation(expectedTest));
        if (expectedTest.getDisabled() != null && expectedTest.getDisabled()) {
            testsToSkip.add(expectedTest);
        }
        if (!testsToSkip.isEmpty()) {
            return;
        }
        this.expectedTest = expectedTest;
        if (assertion.makeTestDeepClone()) {
            test = cloner.clone(expectedTest);
        } else {
            test = expectedTest;
            test.setResponse(null);
            test.setDataStorageDiff(null);
            test.setOutboundMessages(null);
        }
        initializationService.beforeTest(test);
        for (BeforeTest beforeTest : beforeTests) {
            beforeTest.before(test);
        }
        if (testDataStorages != null) {
            testDataStorages.setNewCurrentValue();
        }
    }

    public void runTest() throws Exception {
        if (!testsToSkip.isEmpty()) {
            return;
        }
        waitCompletion.start();
        MockAnswer.enable(() -> {
            try {
                MessageDto<?> message = test.getInboundMessage();
                if (message != null) {
                    testMessageSenders.stream()
                            .filter(s -> s.canSend(message))
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("No service found for message " + message))
                            .sendInboundMessage(message);
                } else {
                    test.executeMethod(beanFactory, (m, r) -> cloner.clone(r));
                }
            } finally {
                waitCompletion.waitCompletion();
            }
        });

        int countMessages = test.getOutboundMessages() == null ? 0 : test.getOutboundMessages().size();
        if (testMessagePoller != null) {
            List<MessageDto<?>> messages = testMessagePoller.pollMessages(countMessages);
            messages.sort(Comparator.comparing(MessageDto::toString));
            test.setOutboundMessages(messages.isEmpty() ? null : messages);
        }
        if (testDataStorages != null) {
            test.setDataStorageDiff(testDataStorages.getMapDiff());
        }
        testConverters.forEach(c -> c.convertNoException(test));

        assertion.assertTestsEquals(testMapper, this.expectedTest, test);
    }

    public void after(Test test) throws Exception {
        if (!testsToSkip.isEmpty()) {
            if (testsToSkip.getLast() == test) {
                testsToSkip.removeLast();
            }
            return;
        }
        initializationService.afterTest(test);
        try {
            for (AfterTest afterTest : afterTests) {
                afterTest.after(test);
            }
        } finally {
            this.test = null;
            this.expectedTest = null;
        }
    }
}
