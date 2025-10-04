package io.github.dimkich.integration.testing.execution;

import io.github.dimkich.integration.testing.*;
import io.github.dimkich.integration.testing.assertion.AssertionConfig;
import io.github.dimkich.integration.testing.execution.junit.ExecutionListener;
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
import org.junit.jupiter.api.Assumptions;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@Slf4j
@Setter
public class TestExecutor {
    private static boolean testsTempDirCleared = false;

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

    @Getter
    private Path testsDir;
    @Getter
    private Test test;
    @Setter
    private ExecutionListener executionListener;
    @Getter
    @Setter
    private Test lastTest;

    public void before(Test expectedTest) throws Exception {
        test = expectedTest;
        log.info(">>> {}", test.getFullName());
        log.info(testMapper.getCurrentPathAndLocation(test));
        if (test.getCalculatedDisabled()) {
            return;
        }
        assertion.setExpected(test);
        test.setResponse(null);
        test.setDataStorageDiff(null);
        test.setOutboundMessages(null);
        this.test.before(this::beforeConsumer, this::afterConsumer);
        if (testDataStorages != null) {
            testDataStorages.setNewCurrentValue();
        }
    }

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

    public void runTest() throws Exception {
        if (test.getCalculatedDisabled()) {
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

        assertion.assertTestsEquals(test);
    }

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

    private void afterConsumer(Test t) throws Exception {
        initializationService.afterTest(t);
        for (AfterTest afterTest : afterTests) {
            afterTest.after(t);
        }
        if (t.getParentTest() == null) {
            assertion.afterTests(t);
        }
    }

    public MockInvoke search(String mockName, String method, List<Object> args) {
        return test.getParentsAndItselfDesc()
                .map(tc -> tc.search(mockName, method, args))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    public void addMockInvoke(MockInvoke invoke) {
        test.getMockInvoke().add(invoke);
    }
}
