package io.github.dimkich.integration.testing.execution.junit;

import io.github.dimkich.integration.testing.assertion.FileOperations;
import io.github.dimkich.integration.testing.execution.TestCaseBeanMocks;
import io.github.dimkich.integration.testing.execution.TestCaseStaticMock;
import io.github.dimkich.integration.testing.openapi.TestOpenAPI;
import lombok.Getter;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

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
    private static List<TestCaseStaticMock> staticMocks = List.of();

    @Override
    public void beforeAll(ExtensionContext context) {
        testThread = Thread.currentThread();
        TestCaseBeanMocks mocks = context.getRequiredTestClass().getAnnotation(TestCaseBeanMocks.class);
        if (mocks != null) {
            mockClasses = Set.of(mocks.mockClasses());
            mockNames = Set.of(mocks.mockNames());
            spyClasses = Set.of(mocks.spyClasses());
            spyNames = Set.of(mocks.spyNames());
        }
        testOpenAPIS = List.of(context.getRequiredTestClass().getAnnotationsByType(TestOpenAPI.class));
        staticMocks = List.of(context.getRequiredTestClass().getAnnotationsByType(TestCaseStaticMock.class));
        if (!initialized) {
            new FileOperations().clearTestsDir();
            initialized = true;
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
