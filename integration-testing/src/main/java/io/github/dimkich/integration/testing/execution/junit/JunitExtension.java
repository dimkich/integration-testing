package io.github.dimkich.integration.testing.execution.junit;

import io.github.dimkich.integration.testing.assertion.FileOperations;
import io.github.dimkich.integration.testing.date.time.MockJavaTime;
import io.github.dimkich.integration.testing.date.time.MockJavaTimeSetUp;
import io.github.dimkich.integration.testing.execution.TestBeanMock;
import io.github.dimkich.integration.testing.execution.TestConstructorMock;
import io.github.dimkich.integration.testing.execution.TestStaticMock;
import io.github.dimkich.integration.testing.execution.mokito.MockitoGlobal;
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

public class JunitExtension implements BeforeAllCallback, AfterAllCallback {
    private static boolean initialized = false;
    @Getter
    private static List<TestOpenAPI> testOpenAPIS = List.of();
    @Getter
    private static List<TestRestTemplate> testRestTemplates = List.of();
    @Getter
    private static List<TestBeanMock> beanMocks = List.of();
    @Getter
    private static List<TestConstructorMock> constructorMocks = List.of();
    @Getter
    private static List<TestStaticMock> staticMocks = List.of();
    @Getter
    private static SpringBootTest springBootTest;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        springBootTest = context.getRequiredTestClass().getAnnotation(SpringBootTest.class);
        if (springBootTest == null) {
            throw new IllegalStateException("No SpringBootTest Annotation found");
        }
        MockitoGlobal.start();
        ClonerAgentSetUp.setClonerInstrumentationIfNone(ByteBuddyAgent.install());
        MockJavaTime mockJavaTime = context.getRequiredTestClass().getAnnotation(MockJavaTime.class);
        if (mockJavaTime != null) {
            MockJavaTimeSetUp.setUp(mockJavaTime);
        }
        testOpenAPIS = List.of(context.getRequiredTestClass().getAnnotationsByType(TestOpenAPI.class));
        beanMocks = List.of(context.getRequiredTestClass().getAnnotationsByType(TestBeanMock.class));
        constructorMocks = List.of(context.getRequiredTestClass().getAnnotationsByType(TestConstructorMock.class));
        staticMocks = List.of(context.getRequiredTestClass().getAnnotationsByType(TestStaticMock.class));
        testRestTemplates = List.of(context.getRequiredTestClass().getAnnotationsByType(TestRestTemplate.class));
        if (!initialized) {
            new FileOperations().clearTestsDir();
            initialized = true;
        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        MockitoGlobal.stop();
        testOpenAPIS = List.of();
        testRestTemplates = List.of();
        beanMocks = List.of();
        constructorMocks = List.of();
        staticMocks = List.of();
    }
}