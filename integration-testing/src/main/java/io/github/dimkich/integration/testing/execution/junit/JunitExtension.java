package io.github.dimkich.integration.testing.execution.junit;

import io.github.dimkich.integration.testing.assertion.FileOperations;
import io.github.dimkich.integration.testing.date.time.MockJavaTime;
import io.github.dimkich.integration.testing.date.time.MockJavaTimeSetUp;
import io.github.dimkich.integration.testing.execution.TestCaseBeanMocks;
import io.github.dimkich.integration.testing.execution.TestCaseStaticMock;
import io.github.dimkich.integration.testing.openapi.TestOpenAPI;
import io.github.dimkich.integration.testing.web.TestRestTemplate;
import io.github.sugarcubes.cloner.ClonerAgentSetUp;
import lombok.Getter;
import net.bytebuddy.agent.ByteBuddyAgent;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Set;

public class JunitExtension implements BeforeAllCallback, AfterAllCallback {
    private static boolean initialized = false;
    @Getter
    private static Thread testThread;
    @Getter
    private static Set<Class<?>> mockClasses = Set.of();
    @Getter
    private static Set<String> mockNames = Set.of();
    @Getter
    private static Set<Class<?>> spyClasses = Set.of();
    @Getter
    private static Set<String> spyNames = Set.of();
    @Getter
    private static List<TestOpenAPI> testOpenAPIS = List.of();
    @Getter
    private static List<TestRestTemplate> testRestTemplates = List.of();
    @Getter
    private static List<TestCaseStaticMock> staticMocks = List.of();
    @Getter
    private static SpringBootTest springBootTest;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        testThread = Thread.currentThread();
        ClonerAgentSetUp.setClonerInstrumentationIfNone(ByteBuddyAgent.install());
        MockJavaTime mockJavaTime = context.getRequiredTestClass().getAnnotation(MockJavaTime.class);
        if (mockJavaTime != null) {
            MockJavaTimeSetUp.setUp(mockJavaTime);
        }
        TestCaseBeanMocks mocks = context.getRequiredTestClass().getAnnotation(TestCaseBeanMocks.class);
        if (mocks != null) {
            mockClasses = Set.of(mocks.mockClasses());
            mockNames = Set.of(mocks.mockNames());
            spyClasses = Set.of(mocks.spyClasses());
            spyNames = Set.of(mocks.spyNames());
        }
        testOpenAPIS = List.of(context.getRequiredTestClass().getAnnotationsByType(TestOpenAPI.class));
        staticMocks = List.of(context.getRequiredTestClass().getAnnotationsByType(TestCaseStaticMock.class));
        testRestTemplates = List.of(context.getRequiredTestClass().getAnnotationsByType(TestRestTemplate.class));
        if (!initialized) {
            new FileOperations().clearTestsDir();
            initialized = true;
        }
        springBootTest = context.getRequiredTestClass().getAnnotation(SpringBootTest.class);
        if (springBootTest == null) {
            throw new IllegalStateException("No SpringBootTest Annotation found");
        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        testThread = null;
        mockClasses = Set.of();
        mockNames = Set.of();
        spyClasses = Set.of();
        spyNames = Set.of();
        testOpenAPIS = List.of();
        testRestTemplates = List.of();
        staticMocks = List.of();
    }

    public static boolean isMock(Class<?> cls, String beanName) {
        return mockClasses.contains(cls) || spyClasses.contains(cls) || mockNames.contains(beanName)
               || spyNames.contains(beanName);
    }

    public static boolean isSpy(Class<?> cls, String beanName) {
        return spyClasses.contains(cls) || spyNames.contains(beanName);
    }

    public static boolean isRunningTestThread() {
        return Thread.currentThread() == testThread;
    }
}
